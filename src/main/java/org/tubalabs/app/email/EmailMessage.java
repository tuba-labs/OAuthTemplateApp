package org.tubalabs.app.email;

import lombok.NonNull;

public record EmailMessage(
        @NonNull String recipient,
        @NonNull String subject,
        @NonNull String body) {
}
