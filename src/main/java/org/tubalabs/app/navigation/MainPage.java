package org.tubalabs.app.navigation;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;

public record MainPage(@NonNull PageModel model, @NonNull List<SubPage> subPages) implements NavigablePage {

    public MainPage {
        Objects.requireNonNull(model, "model");
        subPages = List.copyOf(Objects.requireNonNull(subPages, "subPages"));
    }

    public MainPage(@NonNull PageModel model) {
        this(model, List.of());
    }
}
