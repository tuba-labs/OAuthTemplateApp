package org.tubalabs.app.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tubalabs.app.users.settings.UserSettingsDbo;
import org.tubalabs.app.users.settings.UserSettingsRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RememberLoginPromptServiceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-05-23T12:00:00Z");
    private static final Duration PROMPT_SKIP_DURATION = Duration.ofDays(30);
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(183);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final UserSettingsRepository userSettingsRepository = Mockito.mock(UserSettingsRepository.class);
    private final RememberLoginPromptService rememberLoginPromptService = new RememberLoginPromptService(
            CLOCK,
            new RememberLoginProperties(PROMPT_SKIP_DURATION, TOKEN_VALIDITY),
            userSettingsRepository);

    @Test
    void asksWhenUserHasNoSettings() {
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(rememberLoginPromptService.shouldAsk(USER_ID)).isTrue();
    }

    @Test
    void doesNotAskWhenPromptSkipIsStillActive() {
        final Timestamp askAfter = Timestamp.from(NOW.plus(Duration.ofDays(1)));
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings(askAfter)));

        assertThat(rememberLoginPromptService.shouldAsk(USER_ID)).isFalse();
    }

    @Test
    void asksWhenPromptSkipIsDue() {
        final Timestamp askAfter = Timestamp.from(NOW.minus(Duration.ofSeconds(1)));
        when(userSettingsRepository.findByUserId(USER_ID)).thenReturn(Optional.of(settings(askAfter)));

        assertThat(rememberLoginPromptService.shouldAsk(USER_ID)).isTrue();
    }

    @Test
    void rememberingSkipStoresNextPromptTime() {
        final ArgumentCaptor<Timestamp> askAfter = ArgumentCaptor.forClass(Timestamp.class);

        rememberLoginPromptService.rememberSkip(USER_ID);

        verify(userSettingsRepository).upsertRememberLoginPromptAfter(Mockito.eq(USER_ID), askAfter.capture());
        assertThat(askAfter.getValue().toInstant()).isEqualTo(NOW.plus(PROMPT_SKIP_DURATION));
    }

    @Test
    void clearingSkipClearsPromptTime() {
        rememberLoginPromptService.clearSkip(USER_ID);

        verify(userSettingsRepository).clearRememberLoginPromptAfter(USER_ID);
    }

    private UserSettingsDbo settings(Timestamp rememberLoginPromptAfter) {
        return new UserSettingsDbo(USER_ID, rememberLoginPromptAfter);
    }
}
