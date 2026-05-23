package org.tubalabs.app.users.profile.profilepicture;

import lombok.NonNull;

public class ProfilePictureStorageException extends RuntimeException {

    private final String userMessage;

    public ProfilePictureStorageException(@NonNull String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    public ProfilePictureStorageException(@NonNull String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.userMessage = userMessage;
    }

    public String userMessage() {
        return userMessage;
    }
}
