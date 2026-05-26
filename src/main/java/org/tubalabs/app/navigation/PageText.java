package org.tubalabs.app.navigation;

import lombok.NonNull;

public record PageText(
        @NonNull String labelLocalizationKey,
        @NonNull String tooltipLocalizationKey) {
}
