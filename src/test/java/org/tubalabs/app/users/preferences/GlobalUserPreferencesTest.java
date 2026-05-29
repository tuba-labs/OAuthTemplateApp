package org.tubalabs.app.users.preferences;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferenceValues;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferences;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalUserPreferencesTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String LANGUAGE_PREFERENCE_KEY = "language";
    private static final String DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY = "disable_background_animation";
    private static final String DISABLE_BACKGROUND_ANIMATION_VALUE = Boolean.TRUE.toString();

    private final UserPreferenceRepository userPreferenceRepository = Mockito.mock(UserPreferenceRepository.class);
    private final GlobalUserPreferences globalUserPreferences = new GlobalUserPreferences(userPreferenceRepository);

    @Test
    void usesDefaultsWhenUserHasNoPreferences() {
        when(userPreferenceRepository.findByUserId(USER_ID)).thenReturn(List.of());

        final GlobalUserPreferenceValues preferences = globalUserPreferences.preferences(USER_ID);

        assertThat(preferences.language()).isEqualTo(UserLanguage.DEFAULT);
        assertThat(preferences.disableBackgroundAnimation()).isFalse();
    }

    @Test
    void readsTypedPreferences() {
        when(userPreferenceRepository.findByUserId(USER_ID)).thenReturn(List.of(
                new UserPreferenceDbo(
                        USER_ID,
                        LANGUAGE_PREFERENCE_KEY,
                        UserLanguage.NORWEGIAN.tag()),
                new UserPreferenceDbo(
                        USER_ID,
                        DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY,
                        DISABLE_BACKGROUND_ANIMATION_VALUE)));

        final GlobalUserPreferenceValues preferences = globalUserPreferences.preferences(USER_ID);

        assertThat(preferences.language()).isEqualTo(UserLanguage.NORWEGIAN);
        assertThat(preferences.disableBackgroundAnimation()).isTrue();
    }

    @Test
    void updatesTypedPreferences() {
        globalUserPreferences.updateLanguage(USER_ID, UserLanguage.NORWEGIAN);
        globalUserPreferences.updateDisableBackgroundAnimation(USER_ID, true);

        verify(userPreferenceRepository).upsert(
                USER_ID,
                LANGUAGE_PREFERENCE_KEY,
                UserLanguage.NORWEGIAN.tag());
        verify(userPreferenceRepository).upsert(
                USER_ID,
                DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY,
                DISABLE_BACKGROUND_ANIMATION_VALUE);
    }
}
