package org.tubalabs.app.ui.rememberlogin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.RememberMeServices;
import org.tubalabs.app.security.RememberLoginPromptService;
import org.tubalabs.app.ui.rememberlogin.RememberLoginController;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RememberLoginControllerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String PROVIDER_ID = "local";
    private static final String SUBJECT = "person@example.com";

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver = Mockito.mock(CurrentLoginIdentityResolver.class);
    private final RememberLoginPromptService rememberLoginPromptService = Mockito.mock(RememberLoginPromptService.class);
    private final RememberMeServices rememberMeServices = Mockito.mock(RememberMeServices.class);
    private final RememberLoginController controller =
            new RememberLoginController(currentLoginIdentityResolver, rememberLoginPromptService, rememberMeServices);

    @Test
    void storesIdentityIdAsRememberedUsername() {
        final Authentication authentication = authentication();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.of(identity()));
        final ArgumentCaptor<Authentication> rememberedAuthentication =
                ArgumentCaptor.forClass(Authentication.class);

        final String view = controller.rememberLogin(authentication, request, response);

        assertThat(view).isEqualTo("redirect:/");
        verify(rememberMeServices).loginSuccess(
                Mockito.any(HttpServletRequest.class),
                Mockito.any(HttpServletResponse.class),
                rememberedAuthentication.capture());
        assertThat(rememberedAuthentication.getValue().getName()).isEqualTo(IDENTITY_ID.toString());
        verify(rememberLoginPromptService).clearSkip(USER_ID);
    }

    private Authentication authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                SUBJECT, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private UserIdentityDbo identity() {
        return UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .build();
    }
}
