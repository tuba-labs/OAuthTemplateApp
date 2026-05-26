package org.tubalabs.app.users.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.tubalabs.app.users.profile.validation.ValidDisplayName;

import static org.tubalabs.app.users.profile.UserProfileConstraints.DISPLAY_NAME_MAX_LENGTH;
import static org.tubalabs.app.users.profile.UserProfileConstraints.DISPLAY_NAME_MAX_LENGTH_MESSAGE;

public record UserProfileUpdate(
        @NotBlank(message = "{validation.profile.display-name.required}")
        @ValidDisplayName
        @Size(max = DISPLAY_NAME_MAX_LENGTH, message = DISPLAY_NAME_MAX_LENGTH_MESSAGE)
        String displayName,
        @Size(max = 2000, message = "{validation.profile.picture-url.max-length}")
        String pictureUrl) {

    public UserProfileUpdate {
        displayName = requiredTrimmed(displayName);
        pictureUrl = optionalTrimmed(pictureUrl);
    }

    private static String requiredTrimmed(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private static String optionalTrimmed(String value) {
        final String trimmedValue = requiredTrimmed(value);
        if (trimmedValue == null || trimmedValue.isEmpty()) {
            return null;
        }
        return trimmedValue;
    }
}
