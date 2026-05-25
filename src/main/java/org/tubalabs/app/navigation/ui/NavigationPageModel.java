package org.tubalabs.app.navigation.ui;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.navigation.NavigationCatalog;
import org.tubalabs.app.navigation.NavigablePage;
import org.tubalabs.app.navigation.SubPage;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuDto;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuItemDto;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.ui.profile.logintypes.local.ProfileLocalLoginTypePage;
import org.tubalabs.app.ui.profile.profile.ProfilePage;
import org.tubalabs.app.ui.profile.changepassword.ProfileChangePasswordPage;

import java.util.ArrayList;
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
                .filter(NavigablePage::visibleInNavigation)
                .map(page -> menuItem(page, currentUser, activePage))
                .toList();
        return new NavigationMenuDto(primaryItems, breadcrumbs(currentUser, activePage));
    }

    private NavigationMenuItemDto menuItem(@NonNull NavigablePage page,
                                           @NonNull CurrentUser currentUser,
                                           @NonNull Optional<NavigablePage> activePage) {
        final List<NavigationMenuItemDto> children = page.subPages()
                .stream()
                .filter(subPage -> visibleInNavigation(subPage, currentUser))
                .map(subPage -> menuItem(subPage, currentUser, activePage))
                .toList();
        return new NavigationMenuItemDto(
                label(page, currentUser),
                page.tooltip(),
                page.relativeUrl(),
                children,
                active(page, activePage));
    }

    private String label(@NonNull NavigablePage page, @NonNull CurrentUser currentUser) {
        if (page == ProfilePage.PAGE) {
            return currentUser.navigationDisplayName();
        }
        return page.label();
    }

    private boolean visibleInNavigation(@NonNull NavigablePage page, @NonNull CurrentUser currentUser) {
        return page.visibleInNavigation() && available(page, currentUser);
    }

    private boolean available(@NonNull NavigablePage page, @NonNull CurrentUser currentUser) {
        if (page instanceof SubPage && currentUser.profileSetupRequired()) {
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

    private boolean active(@NonNull NavigablePage page, @NonNull Optional<NavigablePage> activePage) {
        return activePage.map(page::equals).orElse(false);
    }

    private List<NavigationMenuItemDto> breadcrumbs(@NonNull CurrentUser currentUser,
                                                   @NonNull Optional<NavigablePage> activePage) {
        if (activePage.isEmpty()) {
            return List.of();
        }

        for (NavigablePage primaryPage : navigationCatalog.mainPages()) {
            final Optional<List<NavigationMenuItemDto>> trail = breadcrumbTrail(primaryPage, currentUser, activePage);
            if (trail.isPresent()) {
                return trail.get();
            }
        }
        return List.of();
    }

    private Optional<List<NavigationMenuItemDto>> breadcrumbTrail(@NonNull NavigablePage page,
                                                                 @NonNull CurrentUser currentUser,
                                                                 @NonNull Optional<NavigablePage> activePage) {
        if (!available(page, currentUser)) {
            return Optional.empty();
        }
        if (active(page, activePage)) {
            return Optional.of(List.of(menuItemWithoutChildren(page, currentUser, activePage)));
        }

        for (SubPage child : page.subPages()) {
            final Optional<List<NavigationMenuItemDto>> childTrail = breadcrumbTrail(child, currentUser, activePage);
            if (childTrail.isPresent()) {
                final List<NavigationMenuItemDto> trail = new ArrayList<>();
                trail.add(menuItemWithoutChildren(page, currentUser, activePage));
                trail.addAll(childTrail.get());
                return Optional.of(List.copyOf(trail));
            }
        }
        return Optional.empty();
    }

    private NavigationMenuItemDto menuItemWithoutChildren(@NonNull NavigablePage page,
                                                         @NonNull CurrentUser currentUser,
                                                         @NonNull Optional<NavigablePage> activePage) {
        return new NavigationMenuItemDto(
                label(page, currentUser),
                page.tooltip(),
                page.relativeUrl(),
                List.of(),
                active(page, activePage));
    }
}
