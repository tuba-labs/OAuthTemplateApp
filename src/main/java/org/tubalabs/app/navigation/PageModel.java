package org.tubalabs.app.navigation;

import lombok.NonNull;

public record PageModel(
        @NonNull String label,
        @NonNull String tooltip,
        @NonNull String relativeUrl) {

    public PageModel(@NonNull String label, @NonNull String relativeUrl) {
        this(label, label, relativeUrl);
    }
}
