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
    private static final String HOME_LABEL = "Home";
    private static final String LOGIN_TYPES_LABEL = "Login types";
    private static final String CHANGE_PASSWORD_LABEL = "Change password";
    private static final String LINK_EMAIL_PASSWORD_LABEL = "Link email and password";

    private final NavigationPageModel navigationPageModel = new NavigationPageModel(new NavigationCatalog());

    @Test
    void buildsNavigationTreeForLocalCurrentUser() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, EMAIL, false);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile/password");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, DISPLAY_NAME);
        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::relativeUrl)
                .containsExactly("/", "/profile");
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.primaryItems().get(1).active()).isFalse();
        assertThat(navigationMenu.primaryItems().get(1).children())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(LOGIN_TYPES_LABEL, CHANGE_PASSWORD_LABEL);
        assertThat(navigationMenu.primaryItems().get(1).children().get(1).active()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(DISPLAY_NAME, CHANGE_PASSWORD_LABEL);
    }

    @Test
    void hidesLocalLoginLinkFromNavigationForExternalCurrentUser() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, null, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile/login-types");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, DISPLAY_NAME);
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.primaryItems().get(1).active()).isFalse();
        assertThat(navigationMenu.primaryItems().get(1).children())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(LOGIN_TYPES_LABEL);
        assertThat(navigationMenu.primaryItems().get(1).children().getFirst().active()).isTrue();
        assertThat(navigationMenu.primaryItems().get(1).children().getFirst().children()).isEmpty();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(DISPLAY_NAME, LOGIN_TYPES_LABEL);
    }

    @Test
    void hidesProfileSubPagesDuringProfileSetup() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, true, EMAIL, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, DISPLAY_NAME);
        assertThat(navigationMenu.primaryItems().get(1).hasChildren()).isFalse();
        assertThat(navigationMenu.primaryItems().get(1).active()).isTrue();
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(DISPLAY_NAME);
    }

    @Test
    void keepsHomeFirstWhenHomeIsActive() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, EMAIL, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, DISPLAY_NAME);
        assertThat(navigationMenu.primaryItems().getFirst().label()).isEqualTo(HOME_LABEL);
        assertThat(navigationMenu.primaryItems().getFirst().hasChildren()).isFalse();
        assertThat(navigationMenu.primaryItems().getFirst().active()).isTrue();
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL);
    }

    @Test
    void buildsBreadcrumbsForNestedActivePage() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, null, true);

        final NavigationMenuDto navigationMenu =
                navigationPageModel.navigationMenu(currentUser, "/profile/login-types/local/link");

        assertThat(navigationMenu.hasBreadcrumbs()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(DISPLAY_NAME, LOGIN_TYPES_LABEL, LINK_EMAIL_PASSWORD_LABEL);
    }
}
