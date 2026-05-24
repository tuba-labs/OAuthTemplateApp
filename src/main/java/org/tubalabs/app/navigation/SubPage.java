package org.tubalabs.app.navigation;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;

public record SubPage(@NonNull PageModel model, @NonNull List<SubPage> subPages) implements NavigablePage {

    public SubPage {
        Objects.requireNonNull(model, "model");
        subPages = List.copyOf(Objects.requireNonNull(subPages, "subPages"));
    }

    public SubPage(@NonNull PageModel model) {
        this(model, List.of());
    }
}
