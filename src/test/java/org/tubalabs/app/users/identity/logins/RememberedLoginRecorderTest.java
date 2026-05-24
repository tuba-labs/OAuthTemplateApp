package org.tubalabs.app.users.identity.logins;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RememberedLoginRecorderTest {

    private static final Instant NOW = Instant.parse("2026-05-24T10:15:30Z");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final String REMEMBER_ME_KEY = "remember-me-key";

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver = Mockito.mock(CurrentLoginIdentityResolver.class);
    private final UserLoginRepository userLoginRepository = Mockito.mock(UserLoginRepository.class);
    private final RememberedLoginRecorder recorder = new RememberedLoginRecorder(
            Clock.fixed(NOW, ZoneOffset.UTC), currentLoginIdentityResolver, userLoginRepository);

    @AfterEach
    void resetRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsRememberedLogin() {
        final Authentication authentication = rememberedAuthentication();
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.of(identity()));
        setRequest();
        final ArgumentCaptor<UserLoginDbo> login = ArgumentCaptor.forClass(UserLoginDbo.class);

        recorder.recordRememberedLogin(new InteractiveAuthenticationSuccessEvent(
                authentication, RememberMeAuthenticationFilter.class));

        verify(userLoginRepository).insert(login.capture());
        assertThat(login.getValue().id()).isNotNull();
        assertThat(login.getValue().userId()).isEqualTo(USER_ID);
        assertThat(login.getValue().loginTime()).isEqualTo(Timestamp.from(NOW));
        assertThat(login.getValue().providerId()).isEqualTo(PROVIDER_ID);
        assertThat(login.getValue().subject()).isEqualTo(SUBJECT);
        assertThat(login.getValue().clientIp()).isEqualTo(CLIENT_IP);
        assertThat(login.getValue().userAgent()).isEqualTo(USER_AGENT);
    }

    @Test
    void ignoresNonRememberMeAuthentication() {
        final Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated("person@example.com", null, List.of());

        recorder.recordRememberedLogin(new InteractiveAuthenticationSuccessEvent(
                authentication, RememberMeAuthenticationFilter.class));

        verifyNoInteractions(userLoginRepository);
    }

    @Test
    void ignoresEventsNotGeneratedByRememberMeFilter() {
        final Authentication authentication = rememberedAuthentication();

        recorder.recordRememberedLogin(new InteractiveAuthenticationSuccessEvent(
                authentication, UsernamePasswordAuthenticationToken.class));

        verifyNoInteractions(userLoginRepository);
    }

    @Test
    void ignoresRememberedLoginWhenIdentityCannotBeResolved() {
        final Authentication authentication = rememberedAuthentication();
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.empty());

        recorder.recordRememberedLogin(new InteractiveAuthenticationSuccessEvent(
                authentication, RememberMeAuthenticationFilter.class));

        verifyNoInteractions(userLoginRepository);
    }

    private void setRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        request.addHeader("User-Agent", USER_AGENT);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private Authentication rememberedAuthentication() {
        final UserDetails userDetails = User.withUsername(IDENTITY_ID.toString())
                .password("remembered")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
        return new RememberMeAuthenticationToken(REMEMBER_ME_KEY, userDetails, userDetails.getAuthorities());
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
