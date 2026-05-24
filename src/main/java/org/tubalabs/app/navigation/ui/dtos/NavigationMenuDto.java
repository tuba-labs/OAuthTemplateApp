package org.tubalabs.app.navigation.ui.dtos;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;

public record NavigationMenuDto(
        @NonNull List<NavigationMenuItemDto> primaryItems,
        NavigationSectionDto activeSection) {

    public NavigationMenuDto {
        primaryItems = List.copyOf(Objects.requireNonNull(primaryItems, "primaryItems"));
    }

    public boolean hasSectionNavigation() {
        return activeSection != null && activeSection.hasChildItems();
    }

    public String sectionLabel() {
        return activeSection == null ? "" : activeSection.label();
    }

    public List<NavigationMenuItemDto> sectionItems() {
        if (activeSection == null) {
            return List.of();
        }
        return activeSection.menuItems();
    }
}
