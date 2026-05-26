package org.tubalabs.app.navigation;

import lombok.NonNull;

public record PageModel(
        @NonNull PageText text,
        @NonNull String relativeUrl,
        boolean visibleInNavigation,
        @NonNull PageAvailability availability) {

    public PageModel(@NonNull PageText text, @NonNull String relativeUrl) {
        this(text, relativeUrl, true, PageAvailability.ALWAYS);
    }

    public PageModel(@NonNull PageText text, @NonNull String relativeUrl, boolean visibleInNavigation) {
        this(text, relativeUrl, visibleInNavigation, PageAvailability.ALWAYS);
    }

    public PageModel(@NonNull PageText text,
                     @NonNull String relativeUrl,
                     @NonNull PageAvailability availability) {
        this(text, relativeUrl, true, availability);
    }
}
