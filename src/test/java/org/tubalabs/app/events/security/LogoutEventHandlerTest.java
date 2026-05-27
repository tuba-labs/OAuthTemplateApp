package org.tubalabs.app.events.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.tubalabs.app.events.EventLog;
import org.tubalabs.app.events.EventLogService;
import org.tubalabs.app.events.EventType;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LogoutEventHandlerTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver = Mockito.mock(CurrentLoginIdentityResolver.class);
    private final EventLogService eventLogService = Mockito.mock(EventLogService.class);
    private final LogoutEventHandler logoutEventHandler = new LogoutEventHandler(
            currentLoginIdentityResolver,
            eventLogService,
            new UserIdentityEventFactory());

    @Test
    void recordsLogoutWithResolvedIdentity() {
        final Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(SUBJECT, null, List.of());
        final MockHttpServletRequest request = request();
        final ArgumentCaptor<EventLog> eventLog = ArgumentCaptor.forClass(EventLog.class);
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.of(identity()));

        logoutEventHandler.logout(request, new MockHttpServletResponse(), authentication);

        verify(eventLogService).record(eventLog.capture());
        assertThat(eventLog.getValue().eventType()).isEqualTo(EventType.USER_LOGOUT.value());
        assertThat(eventLog.getValue().actorUserId()).isEqualTo(USER_ID);
        assertThat(eventLog.getValue().subjectType()).isEqualTo("user_identity");
        assertThat(eventLog.getValue().subjectId()).isEqualTo(IDENTITY_ID.toString());
        assertThat(eventLog.getValue().clientIp()).isEqualTo(CLIENT_IP);
        assertThat(eventLog.getValue().userAgent()).isEqualTo(USER_AGENT);
        assertThat(eventLog.getValue().details())
                .containsEntry("providerId", PROVIDER_ID)
                .containsEntry("subject", SUBJECT);
    }

    @Test
    void recordsLogoutWithoutResolvedIdentity() {
        final Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(SUBJECT, null, List.of());
        final MockHttpServletRequest request = request();
        final ArgumentCaptor<EventLog> eventLog = ArgumentCaptor.forClass(EventLog.class);
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.empty());

        logoutEventHandler.logout(request, new MockHttpServletResponse(), authentication);

        verify(eventLogService).record(eventLog.capture());
        assertThat(eventLog.getValue().eventType()).isEqualTo(EventType.USER_LOGOUT.value());
        assertThat(eventLog.getValue().actorUserId()).isNull();
        assertThat(eventLog.getValue().subjectType()).isNull();
        assertThat(eventLog.getValue().subjectId()).isNull();
        assertThat(eventLog.getValue().details()).containsEntry("principal", SUBJECT);
    }

    @Test
    void ignoresNullAuthentication() {
        logoutEventHandler.logout(request(), new MockHttpServletResponse(), null);

        verifyNoInteractions(currentLoginIdentityResolver, eventLogService);
    }

    @Test
    void ignoresAnonymousAuthentication() {
        final Authentication authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        logoutEventHandler.logout(request(), new MockHttpServletResponse(), authentication);

        verifyNoInteractions(currentLoginIdentityResolver, eventLogService);
    }

    @Test
    void doesNotFailLogoutWhenEventRecordingFails() {
        final Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(SUBJECT, null, List.of());
        final MockHttpServletRequest request = request();
        when(currentLoginIdentityResolver.identity(authentication)).thenReturn(Optional.of(identity()));
        Mockito.doThrow(new IllegalStateException("Database unavailable"))
                .when(eventLogService)
                .record(Mockito.any());

        assertThatCode(() -> logoutEventHandler.logout(
                request,
                new MockHttpServletResponse(),
                authentication))
                .doesNotThrowAnyException();
    }

    private MockHttpServletRequest request() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        request.addHeader("User-Agent", USER_AGENT);
        return request;
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
