package org.tubalabs.app.ui.profile.logintypes.dtos;

import lombok.NonNull;

public record ProfileLoginTypeOptionDto(
        @NonNull String providerId,
        @NonNull String label) {
}
