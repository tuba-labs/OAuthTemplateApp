package org.tubalabs.app.navigation;

import lombok.NonNull;

import java.util.Optional;

public interface NavigationPageRegistration {

    @NonNull
    PageModel model();

    @NonNull
    default Optional<String> parentRelativeUrl() {
        return Optional.empty();
    }

    default int order() {
        return 0;
    }
}
