package org.tubalabs.app.users.profile.profilepicture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfilePictureStorageServiceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final long MAX_BYTES = 1_000_000;
    private static final int TARGET_SIZE_PIXELS = 600;
    private static final int THUMBNAIL_SIZE_PIXELS = 100;
    private static final int SMALL_TARGET_SIZE_PIXELS = 128;
    private static final int SMALL_THUMBNAIL_SIZE_PIXELS = 32;
    private static final int MAX_DIMENSION_PIXELS = 4096;
    private static final String FILE_PARAMETER = "pictureFile";
    private static final String PNG_FILE_NAME = "avatar.png";
    private static final String SVG_FILE_NAME = "avatar.svg";
    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String SVG_CONTENT_TYPE = "image/svg+xml";
    private static final String PNG_FORMAT = "png";
    private static final String PROFILE_PICTURE_URL = ProfilePictureStorageService.PUBLIC_URL_PREFIX + USER_ID + ".jpg";
    private static final String THUMBNAIL_URL =
            ProfilePictureStorageService.PUBLIC_URL_PREFIX + USER_ID + "-" + THUMBNAIL_SIZE_PIXELS + ".jpg";
    private static final String INVALID_IMAGE_MESSAGE = "Profile picture must be a PNG, JPEG, or GIF image";
    private static final String FILE_TOO_LARGE_MESSAGE = "Profile picture is too large";
    private static final String DIMENSIONS_TOO_LARGE_MESSAGE = "Profile picture dimensions are too large";

    @TempDir
    private Path storageDirectory;

    @Test
    void storesPictureAndReturnsRelativeUrl() throws Exception {
        final ProfilePictureStorageService service = service(MAX_BYTES);
        final MockMultipartFile file = pngFile();

        final String pictureUrl = service.store(USER_ID, file);

        assertThat(pictureUrl).isEqualTo(PROFILE_PICTURE_URL);
        assertThat(Files.exists(storedPath(pictureUrl))).isTrue();
        assertThat(service.thumbnailUrl(pictureUrl)).isEqualTo(THUMBNAIL_URL);
        assertThat(Files.exists(storedPath(THUMBNAIL_URL))).isTrue();
    }

    @Test
    void storesReplacementAtSameUrls() throws Exception {
        final ProfilePictureStorageService service = service(MAX_BYTES);

        final String firstPictureUrl = service.store(USER_ID, pngFile(200, 120));
        final String secondPictureUrl = service.store(USER_ID, pngFile(300, 180));

        assertThat(secondPictureUrl).isEqualTo(firstPictureUrl);
        assertThat(secondPictureUrl).isEqualTo(PROFILE_PICTURE_URL);
        assertThat(Files.exists(storedPath(PROFILE_PICTURE_URL))).isTrue();
        assertThat(Files.exists(storedPath(THUMBNAIL_URL))).isTrue();
    }

    @Test
    void resizesPictureToConfiguredSquareSizes() throws Exception {
        final ProfilePictureStorageService service =
                service(MAX_BYTES, SMALL_TARGET_SIZE_PIXELS, SMALL_THUMBNAIL_SIZE_PIXELS,
                        MAX_DIMENSION_PIXELS, MAX_DIMENSION_PIXELS);
        final MockMultipartFile file = pngFile(900, 500);

        final String pictureUrl = service.store(USER_ID, file);

        final BufferedImage storedImage = ImageIO.read(storedPath(pictureUrl).toFile());
        assertThat(storedImage.getWidth()).isEqualTo(SMALL_TARGET_SIZE_PIXELS);
        assertThat(storedImage.getHeight()).isEqualTo(SMALL_TARGET_SIZE_PIXELS);

        final BufferedImage thumbnailImage = ImageIO.read(storedPath(service.thumbnailUrl(pictureUrl)).toFile());
        assertThat(thumbnailImage.getWidth()).isEqualTo(SMALL_THUMBNAIL_SIZE_PIXELS);
        assertThat(thumbnailImage.getHeight()).isEqualTo(SMALL_THUMBNAIL_SIZE_PIXELS);
    }

    @Test
    void rejectsUnsupportedContentType() {
        final ProfilePictureStorageService service = service(MAX_BYTES);
        final MockMultipartFile file = new MockMultipartFile(
                FILE_PARAMETER, SVG_FILE_NAME, SVG_CONTENT_TYPE, "<svg></svg>".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> service.store(USER_ID, file))
                .isInstanceOf(ProfilePictureStorageException.class)
                .hasMessage(INVALID_IMAGE_MESSAGE);
    }

    @Test
    void rejectsSupportedContentTypeWithInvalidImageBytes() {
        final ProfilePictureStorageService service = service(MAX_BYTES);
        final MockMultipartFile file = new MockMultipartFile(FILE_PARAMETER, PNG_FILE_NAME, PNG_CONTENT_TYPE, new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00});

        assertThatThrownBy(() -> service.store(USER_ID, file))
                .isInstanceOf(ProfilePictureStorageException.class)
                .hasMessage(INVALID_IMAGE_MESSAGE);
    }

    @Test
    void rejectsFileOverConfiguredSize() throws Exception {
        final ProfilePictureStorageService service = service(4);

        assertThatThrownBy(() -> service.store(USER_ID, pngFile()))
                .isInstanceOf(ProfilePictureStorageException.class)
                .hasMessage(FILE_TOO_LARGE_MESSAGE);
    }

    @Test
    void rejectsSourceDimensionsOverConfiguredLimit() throws Exception {
        final ProfilePictureStorageService service =
                service(MAX_BYTES, TARGET_SIZE_PIXELS, THUMBNAIL_SIZE_PIXELS, 100, MAX_DIMENSION_PIXELS);

        assertThatThrownBy(() -> service.store(USER_ID, pngFile(128, 20)))
                .isInstanceOf(ProfilePictureStorageException.class)
                .hasMessage(DIMENSIONS_TOO_LARGE_MESSAGE);
    }

    private ProfilePictureStorageService service(long maxBytes) {
        return service(maxBytes, TARGET_SIZE_PIXELS, THUMBNAIL_SIZE_PIXELS,
                MAX_DIMENSION_PIXELS, MAX_DIMENSION_PIXELS);
    }

    private ProfilePictureStorageService service(long maxBytes,
                                                 int targetSizePixels,
                                                 int thumbnailSizePixels,
                                                 int maxWidthPixels,
                                                 int maxHeightPixels) {
        final ProfilePictureStorageProperties properties = new ProfilePictureStorageProperties(
                storageDirectory.toFile().getAbsolutePath(), maxBytes, targetSizePixels, thumbnailSizePixels, maxWidthPixels, maxHeightPixels);
        return new ProfilePictureStorageService(properties);
    }

    private Path storedPath(String pictureUrl) {
        return storageDirectory.resolve(pictureUrl.substring(ProfilePictureStorageService.PUBLIC_URL_PREFIX.length()));
    }

    private MockMultipartFile pngFile() throws IOException {
        return pngFile(200, 120);
    }

    private MockMultipartFile pngFile(int width, int height) throws IOException {
        return new MockMultipartFile(FILE_PARAMETER, PNG_FILE_NAME, PNG_CONTENT_TYPE, imageBytes(width, height));
    }

    private byte[] imageBytes(int width, int height) throws IOException {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, PNG_FORMAT, outputStream)).isTrue();
        return outputStream.toByteArray();
    }
}
