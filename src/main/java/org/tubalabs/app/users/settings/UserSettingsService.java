package org.tubalabs.app.users.settings;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final @NonNull UserSettingsRepository userSettingsRepository;

    public UserLanguage language(@NonNull UUID userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettingsDbo::languageTag)
                .flatMap(UserLanguage::fromTag)
                .orElse(UserLanguage.DEFAULT);
    }

    public void updateLanguage(@NonNull UUID userId, @NonNull UserLanguage language) {
        userSettingsRepository.upsertLanguageTag(userId, language.tag());
    }
}
