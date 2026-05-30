package org.tubalabs.app.email.outbox.db;

import lombok.Builder;
import lombok.NonNull;

import java.sql.Timestamp;
import java.util.UUID;

@Builder(toBuilder = true)
public record EmailOutboxElementDeliveryDbo(@NonNull UUID emailOutboxElementId,
                                            @NonNull String deliveryStatus,
                                            int attemptCount,
                                            @NonNull Timestamp nextAttemptAt,
                                            @NonNull Timestamp latestDeliveryAt,
                                            Timestamp deliveredAt,
                                            String lastError,
                                            UUID lockToken,
                                            Timestamp lockedUntil) {
}
