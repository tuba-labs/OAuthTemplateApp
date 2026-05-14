package org.tubalabs.app.users.db.user;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record UserDbo(
        @NonNull UUID id) {
}