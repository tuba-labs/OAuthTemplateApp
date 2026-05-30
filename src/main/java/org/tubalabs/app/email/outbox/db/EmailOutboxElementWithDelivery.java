package org.tubalabs.app.email.outbox.db;

import lombok.NonNull;

public record EmailOutboxElementWithDelivery(
        @NonNull EmailOutboxElementDbo element,
        @NonNull EmailOutboxElementDeliveryDbo delivery) {
}
