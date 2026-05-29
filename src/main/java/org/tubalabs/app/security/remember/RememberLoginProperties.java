package org.tubalabs.app.security.remember;

import lombok.NonNull;

import java.time.Duration;
import java.util.Objects;

public record RememberLoginProperties(
        @NonNull Duration promptSkipDuration,
        @NonNull Duration tokenValidity,
        @NonNull String rememberMeKey) {

    public RememberLoginProperties {
        Objects.requireNonNull(promptSkipDuration, "promptSkipDuration");
        Objects.requireNonNull(tokenValidity, "tokenValidity");
        Objects.requireNonNull(rememberMeKey, "rememberMeKey");
        if (promptSkipDuration.isZero() || promptSkipDuration.isNegative()) {
            throw new IllegalArgumentException("Remember-login prompt skip duration must be positive");
        }
        if (tokenValidity.isZero() || tokenValidity.isNegative()) {
            throw new IllegalArgumentException("Remember-me token validity must be positive");
        }
        if (rememberMeKey.isBlank()) {
            throw new IllegalArgumentException("Remember-me key must not be blank");
        }
    }

    public int tokenValiditySeconds() {
        return Math.toIntExact(tokenValidity.toSeconds());
    }
}
