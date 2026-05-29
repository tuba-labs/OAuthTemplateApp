package org.tubalabs.app.users.preferences.global;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tubalabs.app.users.preferences.UserPreferenceDbo;
import org.tubalabs.app.users.preferences.UserPreferenceRepository;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GlobalUserPreferences {

    private static final String LANGUAGE_PREFERENCE_KEY = "language";
    private static final String DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY = "disable_background_animation";

    private final @NonNull UserPreferenceRepository userPreferenceRepository;

    public GlobalUserPreferenceValues preferences(@NonNull UUID userId) {
        final Map<String, String> values = preferenceValues(userId);
        return new GlobalUserPreferenceValues(
                language(values),
                booleanPreference(values, DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY));
    }

    public void updateLanguage(@NonNull UUID userId, @NonNull UserLanguage language) {
        userPreferenceRepository.upsert(userId, LANGUAGE_PREFERENCE_KEY, language.tag());
    }

    public void updateDisableBackgroundAnimation(@NonNull UUID userId, boolean disableBackgroundAnimation) {
        userPreferenceRepository.upsert(
                userId,
                DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY,
                Boolean.toString(disableBackgroundAnimation));
    }

    private Map<String, String> preferenceValues(@NonNull UUID userId) {
        final List<UserPreferenceDbo> preferences = userPreferenceRepository.findByUserId(userId);
        return preferences.stream()
                .collect(Collectors.toMap(
                        UserPreferenceDbo::preferenceKey,
                        UserPreferenceDbo::preferenceValue,
                        (firstValue, secondValue) -> secondValue));
    }

    private UserLanguage language(@NonNull Map<String, String> values) {
        return UserLanguage.fromTag(values.get(LANGUAGE_PREFERENCE_KEY)).orElse(UserLanguage.DEFAULT);
    }

    private boolean booleanPreference(@NonNull Map<String, String> values, @NonNull String key) {
        return Boolean.parseBoolean(values.getOrDefault(key, Boolean.FALSE.toString()));
    }
}
