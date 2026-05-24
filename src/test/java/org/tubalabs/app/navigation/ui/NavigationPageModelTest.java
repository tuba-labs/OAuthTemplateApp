package org.tubalabs.app.navigation.ui;

import org.junit.jupiter.api.Test;
import org.tubalabs.app.navigation.NavigationCatalog;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuDto;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuItemDto;
import org.tubalabs.app.users.current.CurrentUser;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationPageModelTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person Name";
    private static final String EMAIL = "person@example.com";

    private final NavigationPageModel navigationPageModel = new NavigationPageModel(new NavigationCatalog());

    @Test
    void buildsPrimaryNavigationAndProfileSectionForLocalCurrentUser() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, EMAIL, false);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile/password");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly("Home", DISPLAY_NAME);
        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::relativeUrl)
                .containsExactly("/", "/profile");
        assertThat(navigationMenu.hasSectionNavigation()).isTrue();
        assertThat(navigationMenu.sectionLabel()).isEqualTo(DISPLAY_NAME);
        assertThat(navigationMenu.sectionItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(DISPLAY_NAME, "Login types", "Change password");
        assertThat(navigationMenu.sectionItems().getFirst().children()).isEmpty();
        assertThat(navigationMenu.sectionItems().get(1).children()).isEmpty();
        assertThat(navigationMenu.sectionItems().get(2).active()).isTrue();
    }

    @Test
    void includesLocalLoginLinkForExternalCurrentUser() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, null, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile/login-types");

        assertThat(navigationMenu.sectionItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(DISPLAY_NAME, "Login types");
        assertThat(navigationMenu.sectionItems().get(1).active()).isTrue();
        assertThat(navigationMenu.sectionItems().get(1).children())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly("Link email and password");
    }

    @Test
    void hidesProfileSubPagesDuringProfileSetup() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, true, EMAIL, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly("Home", DISPLAY_NAME);
        assertThat(navigationMenu.hasSectionNavigation()).isFalse();
    }
}
