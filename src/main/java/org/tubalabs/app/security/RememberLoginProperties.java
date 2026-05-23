package org.tubalabs.app.security;

import lombok.NonNull;

import java.time.Duration;
import java.util.Objects;

public record RememberLoginProperties(
        @NonNull Duration promptSkipDuration,
        @NonNull Duration tokenValidity) {

    public RememberLoginProperties {
        Objects.requireNonNull(promptSkipDuration, "promptSkipDuration");
        Objects.requireNonNull(tokenValidity, "tokenValidity");
        if (promptSkipDuration.isZero() || promptSkipDuration.isNegative()) {
            throw new IllegalArgumentException("Remember-login prompt skip duration must be positive");
        }
        if (tokenValidity.isZero() || tokenValidity.isNegative()) {
            throw new IllegalArgumentException("Remember-me token validity must be positive");
        }
    }

    public int tokenValiditySeconds() {
        return Math.toIntExact(tokenValidity.toSeconds());
    }
}
