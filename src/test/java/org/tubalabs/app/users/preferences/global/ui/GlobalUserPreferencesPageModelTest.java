package org.tubalabs.app.users.preferences.global.ui;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferenceValues;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalUserPreferencesPageModelTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final GlobalUserPreferencesPageModel pageModel = new GlobalUserPreferencesPageModel();

    @Test
    void mapsLanguageAndBackgroundAnimationPreference() {
        final Model model = new ExtendedModelMap();
        final CurrentUser currentUser = new CurrentUser(
                USER_ID,
                "Person",
                null,
                false,
                null,
                true,
                UserLanguage.NORWEGIAN.tag(),
                true);

        pageModel.addGlobalPreferences(model, currentUser);

        final Object preferences = model.getAttribute(GlobalUserPreferencesPageModel.GLOBAL_USER_PREFERENCES_ATTRIBUTE);
        assertThat(preferences)
                .isEqualTo(new GlobalUserPreferenceValues(UserLanguage.NORWEGIAN, true));
        assertThat(model.getAttribute(GlobalUserPreferencesPageModel.BACKGROUND_ANIMATION_ENABLED_ATTRIBUTE))
                .isEqualTo(Boolean.FALSE);
    }
}
