package org.tubalabs.app.navigation.ui.dtos;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record NavigationSectionDto(
        @NonNull NavigationMenuItemDto parentItem,
        @NonNull List<NavigationMenuItemDto> childItems) {

    public NavigationSectionDto {
        Objects.requireNonNull(parentItem, "parentItem");
        childItems = List.copyOf(Objects.requireNonNull(childItems, "childItems"));
    }

    public String label() {
        return parentItem.label();
    }

    public boolean hasChildItems() {
        return !childItems.isEmpty();
    }

    public List<NavigationMenuItemDto> menuItems() {
        final List<NavigationMenuItemDto> menuItems = new ArrayList<>(childItems.size() + 1);
        menuItems.add(parentItem.withoutChildren());
        menuItems.addAll(childItems);
        return List.copyOf(menuItems);
    }
}
