package org.tubalabs.app.users.profile.api.ui;

import lombok.NonNull;

public record ProfileLinkedLoginTypeOption(
        @NonNull String providerId,
        @NonNull String label,
        boolean current,
        boolean unlinkAvailable,
        @NonNull String status) {
}
