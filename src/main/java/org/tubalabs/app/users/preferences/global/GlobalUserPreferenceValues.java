package org.tubalabs.app.users.preferences.global;

import lombok.NonNull;
import org.tubalabs.app.users.settings.UserLanguage;

public record GlobalUserPreferenceValues(
        @NonNull UserLanguage language,
        boolean disableBackgroundAnimation) {
}
