package org.tubalabs.app.users.profile.profilepicture;

import lombok.NonNull;
import org.tubalabs.app.localization.LocalizationKey;

public enum ProfilePictureStorageFailure implements LocalizationKey {
    EMPTY_FILE("profile.picture.error.empty"),
    FILE_TOO_LARGE("profile.picture.error.file-too-large"),
    INVALID_IMAGE("profile.picture.error.invalid-image"),
    DIMENSIONS_TOO_LARGE("profile.picture.error.dimensions-too-large"),
    INVALID_PATH("profile.picture.error.upload-failed"),
    READ_FAILED("profile.picture.error.upload-failed"),
    STORE_FAILED("profile.picture.error.upload-failed");

    private final String localizationKey;

    ProfilePictureStorageFailure(@NonNull String localizationKey) {
        this.localizationKey = localizationKey;
    }

    @Override
    public String getLocalizationKey() {
        return localizationKey;
    }
}
