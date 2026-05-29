package org.tubalabs.app.users.preferences.global.ui;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferenceValues;
import org.tubalabs.app.users.settings.UserLanguage;

@Component
public class GlobalUserPreferencesPageModel {

    public static final String GLOBAL_USER_PREFERENCES_ATTRIBUTE = "globalUserPreferences";
    public static final String BACKGROUND_ANIMATION_ENABLED_ATTRIBUTE = "backgroundAnimationEnabled";

    public void addGlobalPreferences(@NonNull Model model, @NonNull CurrentUser currentUser) {
        final GlobalUserPreferenceValues preferences = preferences(currentUser);
        model.addAttribute(GLOBAL_USER_PREFERENCES_ATTRIBUTE, preferences);
        model.addAttribute(BACKGROUND_ANIMATION_ENABLED_ATTRIBUTE, !preferences.disableBackgroundAnimation());
    }

    private GlobalUserPreferenceValues preferences(@NonNull CurrentUser currentUser) {
        return new GlobalUserPreferenceValues(
                UserLanguage.fromTagOrDefault(currentUser.languageTag()),
                currentUser.disableBackgroundAnimation());
    }
}
