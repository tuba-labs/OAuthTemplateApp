package org.tubalabs.app.navigation.ui;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuDto;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserRequestContext;
import org.tubalabs.app.users.preferences.global.GlobalUserPreferenceValues;
import org.tubalabs.app.users.preferences.global.ui.GlobalUserPreferencesPageModel;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractNavigationControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String AUTHENTICATED_NAVIGATION_MENU_ATTRIBUTE = "authenticatedNavigationMenu";

    private final CurrentUserRequestContext currentUserRequestContext = Mockito.mock(CurrentUserRequestContext.class);
    private final NavigationPageModel navigationPageModel = Mockito.mock(NavigationPageModel.class);
    private final GlobalUserPreferencesPageModel globalUserPreferencesPageModel = new GlobalUserPreferencesPageModel();
    private final TestNavigationController controller =
            new TestNavigationController(currentUserRequestContext, navigationPageModel, globalUserPreferencesPageModel);

    @Test
    void addsNavigationAndGlobalPreferencesForAuthenticatedUser() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/app");
        request.setRequestURI("/app/profile");
        final Authentication authentication = authenticated();
        final Model model = new ExtendedModelMap();
        final CurrentUser currentUser = new CurrentUser(
                USER_ID,
                "Person",
                null,
                false,
                null,
                true,
                UserLanguage.NORWEGIAN.tag(),
                true);
        final NavigationMenuDto navigationMenu = new NavigationMenuDto(List.of(), List.of());
        when(currentUserRequestContext.authenticated(authentication)).thenReturn(true);
        when(currentUserRequestContext.currentUser(request, authentication)).thenReturn(currentUser);
        when(navigationPageModel.navigationMenu(currentUser, "/profile")).thenReturn(navigationMenu);

        controller.addAuthenticatedNavigation(model, request, authentication);

        assertThat(model.getAttribute(AUTHENTICATED_NAVIGATION_MENU_ATTRIBUTE)).isSameAs(navigationMenu);
        assertThat(model.getAttribute(GlobalUserPreferencesPageModel.GLOBAL_USER_PREFERENCES_ATTRIBUTE))
                .isEqualTo(new GlobalUserPreferenceValues(UserLanguage.NORWEGIAN, true));
        assertThat(model.getAttribute(GlobalUserPreferencesPageModel.BACKGROUND_ANIMATION_ENABLED_ATTRIBUTE))
                .isEqualTo(Boolean.FALSE);
    }

    @Test
    void ignoresAnonymousUser() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final Authentication authentication = authenticated();
        final Model model = new ExtendedModelMap();
        when(currentUserRequestContext.authenticated(authentication)).thenReturn(false);

        controller.addAuthenticatedNavigation(model, request, authentication);

        assertThat(model.containsAttribute(AUTHENTICATED_NAVIGATION_MENU_ATTRIBUTE)).isFalse();
        verify(currentUserRequestContext, never()).currentUser(Mockito.any(), Mockito.any());
    }

    private Authentication authenticated() {
        return UsernamePasswordAuthenticationToken.authenticated("person", null, List.of());
    }

    private static class TestNavigationController extends AbstractNavigationController {

        private TestNavigationController(CurrentUserRequestContext currentUserRequestContext,
                                         NavigationPageModel navigationPageModel,
                                         GlobalUserPreferencesPageModel globalUserPreferencesPageModel) {
            super(currentUserRequestContext, navigationPageModel, globalUserPreferencesPageModel);
        }
    }
}
