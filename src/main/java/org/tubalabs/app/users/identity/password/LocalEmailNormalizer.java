package org.tubalabs.app.users.identity.password;

import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocalEmailNormalizer {

    public String normalize(@NonNull String email) {
        final String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return normalizedEmail;
    }
}
