package org.tubalabs.app.users.profile.profilepicture;

import lombok.NonNull;

public class ProfilePictureStorageException extends RuntimeException {

    private final ProfilePictureStorageFailure reason;

    public ProfilePictureStorageException(@NonNull ProfilePictureStorageFailure reason) {
        super("Profile picture storage failed: " + reason);
        this.reason = reason;
    }

    public ProfilePictureStorageException(@NonNull ProfilePictureStorageFailure reason, Throwable cause) {
        super("Profile picture storage failed: " + reason, cause);
        this.reason = reason;
    }

    public ProfilePictureStorageFailure reason() {
        return reason;
    }
}
