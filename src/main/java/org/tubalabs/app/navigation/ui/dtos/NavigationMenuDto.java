package org.tubalabs.app.navigation.ui.dtos;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;

public record NavigationMenuDto(
        @NonNull List<NavigationMenuItemDto> primaryItems,
        @NonNull List<NavigationMenuItemDto> breadcrumbs) {

    public NavigationMenuDto {
        primaryItems = List.copyOf(Objects.requireNonNull(primaryItems, "primaryItems"));
        breadcrumbs = List.copyOf(Objects.requireNonNull(breadcrumbs, "breadcrumbs"));
    }

    public boolean hasMenuItems() {
        return !primaryItems.isEmpty();
    }

    public boolean hasBreadcrumbs() {
        return !breadcrumbs.isEmpty();
    }
}
