package org.tubalabs.app.ui.profile.logintypes.dtos;

import lombok.NonNull;

public record ProfileLinkedLoginTypeOptionDto(
        @NonNull String providerId,
        @NonNull String label,
        boolean localLoginType,
        boolean passwordChangeAvailable,
        boolean current,
        boolean unlinkAvailable,
        @NonNull String status) {
}
