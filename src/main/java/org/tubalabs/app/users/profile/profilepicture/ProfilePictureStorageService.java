package org.tubalabs.app.users.profile.profilepicture;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfilePictureStorageService {

    public static final String PUBLIC_URL_PREFIX = "/profile-pictures/";
    public static final String PUBLIC_URL_PATTERN = PUBLIC_URL_PREFIX + "**";

    private static final String OUTPUT_FORMAT = "jpg";
    private static final String OUTPUT_EXTENSION = ".jpg";
    private static final String THUMBNAIL_SUFFIX_SEPARATOR = "-";
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/gif");

    private final @NonNull ProfilePictureStorageProperties properties;

    public String store(@NonNull UUID userId, @NonNull MultipartFile file) {
        final byte[] imageBytes = readUploadedBytes(file);
        final String contentType = normalizedContentType(file);
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType) || !hasExpectedHeader(imageBytes, contentType)) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_IMAGE);
        }

        final BufferedImage uploadedImage = decodeImage(imageBytes);
        final BufferedImage profilePicture = resizeToSquare(uploadedImage, properties.targetSizePixels());
        final BufferedImage thumbnailPicture = resizeToSquare(uploadedImage, properties.thumbnailSizePixels());
        final String baseFileName = userId.toString();
        final String fileName = baseFileName + OUTPUT_EXTENSION;
        final String thumbnailFileName = thumbnailFileName(baseFileName);
        final Path storageDirectory = properties.storageDirectory().toAbsolutePath().normalize();
        final Path target = storageDirectory.resolve(fileName).normalize();
        final Path thumbnailTarget = storageDirectory.resolve(thumbnailFileName).normalize();
        if (!target.startsWith(storageDirectory) || !thumbnailTarget.startsWith(storageDirectory)) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_PATH);
        }

        try {
            Files.createDirectories(storageDirectory);
            replaceImage(profilePicture, target);
            replaceImage(thumbnailPicture, thumbnailTarget);
        } catch (IOException exception) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.STORE_FAILED, exception);
        }

        return PUBLIC_URL_PREFIX + fileName;
    }

    public String thumbnailUrl(@NonNull String profilePictureUrl) {
        if (!profilePictureUrl.startsWith(PUBLIC_URL_PREFIX) || !profilePictureUrl.endsWith(OUTPUT_EXTENSION)) {
            return profilePictureUrl;
        }

        final String fileName = profilePictureUrl.substring(PUBLIC_URL_PREFIX.length());
        final String baseFileName = fileName.substring(0, fileName.length() - OUTPUT_EXTENSION.length());
        return PUBLIC_URL_PREFIX + thumbnailFileName(baseFileName);
    }

    private byte[] readUploadedBytes(@NonNull MultipartFile file) {
        if (file.isEmpty()) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.EMPTY_FILE);
        }
        if (file.getSize() > properties.maxBytes()) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.FILE_TOO_LARGE);
        }

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.READ_FAILED, exception);
        }
    }

    private BufferedImage decodeImage(@NonNull byte[] imageBytes) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (imageInputStream == null) {
                throw new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_IMAGE);
            }

            final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
            if (!imageReaders.hasNext()) {
                throw new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_IMAGE);
            }

            final ImageReader imageReader = imageReaders.next();
            try {
                imageReader.setInput(imageInputStream, true, true);
                validateDimensions(imageReader.getWidth(0), imageReader.getHeight(0));
                final BufferedImage image = imageReader.read(0);
                if (image == null) {
                    throw new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_IMAGE);
                }
                return image;
            } finally {
                imageReader.dispose();
            }
        } catch (IOException exception) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_IMAGE);
        }
    }

    private void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.INVALID_IMAGE);
        }
        if (width > properties.maxWidthPixels() || height > properties.maxHeightPixels()) {
            throw new ProfilePictureStorageException(ProfilePictureStorageFailure.DIMENSIONS_TOO_LARGE);
        }
    }

    private BufferedImage resizeToSquare(@NonNull BufferedImage sourceImage, int targetSizePixels) {
        final int sourceWidth = sourceImage.getWidth();
        final int sourceHeight = sourceImage.getHeight();
        final int sourceSize = Math.min(sourceWidth, sourceHeight);
        final int sourceX = (sourceWidth - sourceSize) / 2;
        final int sourceY = (sourceHeight - sourceSize) / 2;

        final BufferedImage resizedImage =
                new BufferedImage(targetSizePixels, targetSizePixels, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetSizePixels, targetSizePixels);
            graphics.drawImage(sourceImage, 0, 0, targetSizePixels, targetSizePixels,
                    sourceX, sourceY, sourceX + sourceSize, sourceY + sourceSize, null);
            return resizedImage;
        } finally {
            graphics.dispose();
        }
    }

    private void replaceImage(@NonNull BufferedImage image, @NonNull Path target) throws IOException {
        final Path temporaryTarget = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            if (!ImageIO.write(image, OUTPUT_FORMAT, temporaryTarget.toFile())) {
                throw new IOException("No image writer found for " + OUTPUT_FORMAT);
            }
            Files.move(temporaryTarget, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            deleteIfExists(temporaryTarget);
        }
    }

    private String thumbnailFileName(@NonNull String baseFileName) {
        return baseFileName + THUMBNAIL_SUFFIX_SEPARATOR + properties.thumbnailSizePixels() + OUTPUT_EXTENSION;
    }

    private void deleteIfExists(@NonNull Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private String normalizedContentType(@NonNull MultipartFile file) {
        if (file.getContentType() == null) {
            return "";
        }
        return file.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasExpectedHeader(@NonNull byte[] imageBytes, @NonNull String contentType) {
        return switch (contentType) {
            case "image/png" -> startsWith(imageBytes, new byte[]{
                    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
            case "image/jpeg" -> imageBytes.length >= 3
                    && imageBytes[0] == (byte) 0xff
                    && imageBytes[1] == (byte) 0xd8
                    && imageBytes[2] == (byte) 0xff;
            case "image/gif" -> startsWith(imageBytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(imageBytes, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            default -> false;
        };
    }

    private boolean startsWith(@NonNull byte[] value, @NonNull byte[] expectedPrefix) {
        if (value.length < expectedPrefix.length) {
            return false;
        }
        for (int index = 0; index < expectedPrefix.length; index++) {
            if (value[index] != expectedPrefix[index]) {
                return false;
            }
        }
        return true;
    }
}
