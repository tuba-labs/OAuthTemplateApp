package org.tubalabs.app.users.profile.profilepicture;

public enum ProfilePictureStorageFailure {
    EMPTY_FILE,
    FILE_TOO_LARGE,
    INVALID_IMAGE,
    DIMENSIONS_TOO_LARGE,
    INVALID_PATH,
    READ_FAILED,
    STORE_FAILED
}
