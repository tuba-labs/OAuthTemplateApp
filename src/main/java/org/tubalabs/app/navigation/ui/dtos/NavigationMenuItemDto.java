package org.tubalabs.app.navigation.ui.dtos;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;

public record NavigationMenuItemDto(
        @NonNull String label,
        @NonNull String tooltip,
        String relativeUrl,
        @NonNull List<NavigationMenuItemDto> children,
        boolean active) {

    public NavigationMenuItemDto {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(tooltip, "tooltip");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    public boolean hasRelativeUrl() {
        return relativeUrl != null && !relativeUrl.isBlank();
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
