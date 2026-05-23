package org.tubalabs.app.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.settings.UserSettingsDbo;
import org.tubalabs.app.users.settings.UserSettingsRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RememberLoginPromptService {

    private final Clock clock;
    private final RememberLoginProperties rememberLoginProperties;
    private final UserSettingsRepository userSettingsRepository;

    public boolean shouldAsk(@NonNull UUID userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettingsDbo::rememberLoginPromptAfter)
                .map(Timestamp::toInstant)
                .map(askAfter -> !askAfter.isAfter(clock.instant()))
                .orElse(true);
    }

    public void rememberSkip(@NonNull UUID userId) {
        final Instant askAfter = clock.instant().plus(rememberLoginProperties.promptSkipDuration());
        userSettingsRepository.upsertRememberLoginPromptAfter(userId, Timestamp.from(askAfter));
    }

    public void clearSkip(@NonNull UUID userId) {
        userSettingsRepository.clearRememberLoginPromptAfter(userId);
    }
}
