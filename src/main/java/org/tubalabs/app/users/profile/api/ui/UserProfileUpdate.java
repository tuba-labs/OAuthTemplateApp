package org.tubalabs.app.users.profile.api.ui;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.tubalabs.app.users.profile.validation.ValidDisplayName;

public record UserProfileUpdate(
        @NotBlank(message = "Display name is required")
        @ValidDisplayName
        @Size(max = 80, message = "Display name must be 80 characters or fewer")
        String displayName,
        @Size(max = 2000, message = "Picture URL must be 2000 characters or fewer")
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
