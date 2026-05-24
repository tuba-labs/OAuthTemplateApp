package org.tubalabs.app.users.identity.password.api.dtos;

import lombok.NonNull;

import java.util.UUID;

public record LocalRegistrationResponseDto(@NonNull UUID userId, @NonNull String email) {
}
