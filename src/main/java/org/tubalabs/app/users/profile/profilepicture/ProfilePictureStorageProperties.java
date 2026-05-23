package org.tubalabs.app.users.profile.profilepicture;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ProfilePictureStorageProperties {

    private final Path storageDirectory;
    private final long maxBytes;
    private final int targetSizePixels;
    private final int thumbnailSizePixels;
    private final int maxWidthPixels;
    private final int maxHeightPixels;

    @Autowired
    public ProfilePictureStorageProperties(@Value("${app.profile.pictures.storage-dir}") @NonNull String storageDirectory,
                                           @Value("${app.profile.pictures.max-bytes}") long maxBytes,
                                           @Value("${app.profile.pictures.target-size-pixels}") int targetSizePixels,
                                           @Value("${app.profile.pictures.thumbnail-size-pixels}") int thumbnailSizePixels,
                                           @Value("${app.profile.pictures.max-width-pixels}") int maxWidthPixels,
                                           @Value("${app.profile.pictures.max-height-pixels}") int maxHeightPixels) {
        this(Path.of(storageDirectory), maxBytes, targetSizePixels, thumbnailSizePixels, maxWidthPixels, maxHeightPixels);
    }

    ProfilePictureStorageProperties(@NonNull Path storageDirectory, long maxBytes) {
        this(storageDirectory, maxBytes, 600, 100, 4096, 4096);
    }

    ProfilePictureStorageProperties(@NonNull Path storageDirectory,
                                    long maxBytes,
                                    int targetSizePixels,
                                    int thumbnailSizePixels,
                                    int maxWidthPixels,
                                    int maxHeightPixels) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("Profile picture max bytes must be positive");
        }
        if (targetSizePixels <= 0) {
            throw new IllegalArgumentException("Profile picture target size must be positive");
        }
        if (thumbnailSizePixels <= 0) {
            throw new IllegalArgumentException("Profile picture thumbnail size must be positive");
        }
        if (maxWidthPixels <= 0 || maxHeightPixels <= 0) {
            throw new IllegalArgumentException("Profile picture max dimensions must be positive");
        }
        this.storageDirectory = storageDirectory;
        this.maxBytes = maxBytes;
        this.targetSizePixels = targetSizePixels;
        this.thumbnailSizePixels = thumbnailSizePixels;
        this.maxWidthPixels = maxWidthPixels;
        this.maxHeightPixels = maxHeightPixels;
    }

    public Path storageDirectory() {
        return storageDirectory;
    }

    public long maxBytes() {
        return maxBytes;
    }

    public int targetSizePixels() {
        return targetSizePixels;
    }

    public int thumbnailSizePixels() {
        return thumbnailSizePixels;
    }

    public int maxWidthPixels() {
        return maxWidthPixels;
    }

    public int maxHeightPixels() {
        return maxHeightPixels;
    }

    public String resourceLocation() {
        final String location = storageDirectory.toUri().toString();
        if (location.endsWith("/")) {
            return location;
        }
        return location + "/";
    }
}
