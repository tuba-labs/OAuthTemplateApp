package org.tubalabs.app.navigation.ui;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.navigation.NavigationCatalog;
import org.tubalabs.app.navigation.NavigablePage;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuDto;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuItemDto;
import org.tubalabs.app.navigation.ui.dtos.NavigationSectionDto;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.ui.profile.logintypes.local.ProfileLocalLoginTypePage;
import org.tubalabs.app.ui.profile.profile.ProfilePage;
import org.tubalabs.app.ui.profile.changepassword.ProfileChangePasswordPage;

import java.util.List;
import java.util.Optional;

@Component
public class NavigationPageModel {

    private final NavigationCatalog navigationCatalog;

    public NavigationPageModel(@NonNull NavigationCatalog navigationCatalog) {
        this.navigationCatalog = navigationCatalog;
    }

    public NavigationMenuDto navigationMenu(@NonNull CurrentUser currentUser, @NonNull String currentPath) {
        final Optional<NavigablePage> activePage = navigationCatalog.activePage(currentPath);
        final List<NavigationMenuItemDto> primaryItems = navigationCatalog.mainPages()
                .stream()
                .map(page -> menuItem(page, currentUser, activePage))
                .toList();
        return menu(primaryItems);
    }

    private NavigationMenuItemDto menuItem(@NonNull NavigablePage page,
                                           @NonNull CurrentUser currentUser,
                                           @NonNull Optional<NavigablePage> activePage) {
        final List<NavigationMenuItemDto> children = page.subPages()
                .stream()
                .filter(subPage -> visible(subPage, currentUser))
                .map(subPage -> menuItem(subPage, currentUser, activePage))
                .toList();
        return new NavigationMenuItemDto(
                label(page, currentUser),
                page.tooltip(),
                page.relativeUrl(),
                children,
                active(page, children, activePage));
    }

    private NavigationMenuDto menu(@NonNull List<NavigationMenuItemDto> primaryItems) {
        final NavigationMenuItemDto activePrimaryItem = primaryItems.stream()
                .filter(NavigationMenuItemDto::active)
                .findFirst()
                .orElse(null);
        if (activePrimaryItem == null || !activePrimaryItem.hasChildren()) {
            return new NavigationMenuDto(primaryItems, null);
        }
        return new NavigationMenuDto(
                primaryItems,
                new NavigationSectionDto(activePrimaryItem, activePrimaryItem.children()));
    }

    private String label(@NonNull NavigablePage page, @NonNull CurrentUser currentUser) {
        if (page == ProfilePage.PAGE) {
            return currentUser.navigationDisplayName();
        }
        return page.label();
    }

    private boolean visible(@NonNull NavigablePage page, @NonNull CurrentUser currentUser) {
        if (currentUser.profileSetupRequired()) {
            return false;
        }
        if (page == ProfileChangePasswordPage.PAGE) {
            return currentUser.passwordChangeAvailable();
        }
        if (page == ProfileLocalLoginTypePage.PAGE) {
            return currentUser.localLoginLinkAvailable();
        }
        return true;
    }

    private boolean active(@NonNull NavigablePage page,
                           @NonNull List<NavigationMenuItemDto> children,
                           @NonNull Optional<NavigablePage> activePage) {
        return activePage.map(page::equals).orElse(false)
                || children.stream().anyMatch(NavigationMenuItemDto::active);
    }
}
