package org.tubalabs.app.email.outbox.db;

import lombok.Builder;
import lombok.NonNull;

import java.util.UUID;

@Builder(toBuilder = true)
public record EmailOutboxElementDbo(
        @NonNull UUID id,
        @NonNull String recipient,
        @NonNull String subject,
        @NonNull String body) {
}
