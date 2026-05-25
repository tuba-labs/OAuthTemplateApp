package org.tubalabs.app.navigation;

import lombok.NonNull;

public record PageModel(
        @NonNull String label,
        @NonNull String tooltip,
        @NonNull String relativeUrl,
        boolean visibleInNavigation) {

    public PageModel(@NonNull String label, @NonNull String tooltip, @NonNull String relativeUrl) {
        this(label, tooltip, relativeUrl, true);
    }

    public PageModel(@NonNull String label, @NonNull String relativeUrl) {
        this(label, label, relativeUrl, true);
    }
}
