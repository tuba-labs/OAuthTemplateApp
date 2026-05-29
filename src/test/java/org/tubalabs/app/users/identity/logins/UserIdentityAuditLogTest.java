package org.tubalabs.app.users.identity.logins;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tubalabs.app.events.EventLog;
import org.tubalabs.app.events.EventType;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class UserIdentityAuditLogTest {

    private static final Instant NOW = Instant.parse("2026-05-24T10:15:30Z");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";

    private final UserIdentityAuditWriter userIdentityAuditWriter = Mockito.mock(UserIdentityAuditWriter.class);
    private final UserIdentityAuditLog userIdentityAuditLog = new UserIdentityAuditLog(
            Clock.fixed(NOW, ZoneOffset.UTC),
            userIdentityAuditWriter,
            new UserIdentityEventFactory());

    @Test
    void recordsLoginRowAndEvent() {
        final UserIdentityDbo identity = identity();
        final ArgumentCaptor<UserLoginDbo> login = ArgumentCaptor.forClass(UserLoginDbo.class);
        final ArgumentCaptor<EventLog> eventLog = ArgumentCaptor.forClass(EventLog.class);

        userIdentityAuditLog.recordLogin(identity, CLIENT_IP, USER_AGENT, true, false);

        verify(userIdentityAuditWriter).insertLogin(login.capture());
        assertThat(login.getValue().id()).isNotNull();
        assertThat(login.getValue().userId()).isEqualTo(USER_ID);
        assertThat(login.getValue().loginTime()).isEqualTo(Timestamp.from(NOW));
        assertThat(login.getValue().providerId()).isEqualTo(PROVIDER_ID);
        assertThat(login.getValue().subject()).isEqualTo(SUBJECT);
        assertThat(login.getValue().clientIp()).isEqualTo(CLIENT_IP);
        assertThat(login.getValue().userAgent()).isEqualTo(USER_AGENT);

        verify(userIdentityAuditWriter).insertEvent(eventLog.capture());
        assertThat(eventLog.getValue().eventType()).isEqualTo(EventType.USER_LOGIN.value());
        assertThat(eventLog.getValue().actorUserId()).isEqualTo(USER_ID);
        assertThat(eventLog.getValue().subjectType()).isEqualTo("user_identity");
        assertThat(eventLog.getValue().subjectId()).isEqualTo(IDENTITY_ID.toString());
        assertThat(eventLog.getValue().clientIp()).isEqualTo(CLIENT_IP);
        assertThat(eventLog.getValue().userAgent()).isEqualTo(USER_AGENT);
        assertThat(eventLog.getValue().details())
                .containsEntry("providerId", PROVIDER_ID)
                .containsEntry("subject", SUBJECT)
                .containsEntry("remembered", true)
                .containsEntry("newUser", false);
    }

    @Test
    void continuesToEventLogWhenLoginRowFails() {
        doThrow(new IllegalStateException("login failed"))
                .when(userIdentityAuditWriter)
                .insertLogin(Mockito.any(UserLoginDbo.class));

        assertThatNoException().isThrownBy(() -> userIdentityAuditLog.recordLogin(
                identity(), CLIENT_IP, USER_AGENT, true, false));

        verify(userIdentityAuditWriter).insertEvent(Mockito.any(EventLog.class));
    }

    @Test
    void swallowsEventLogFailure() {
        doThrow(new IllegalStateException("event failed"))
                .when(userIdentityAuditWriter)
                .insertEvent(Mockito.any(EventLog.class));

        assertThatNoException().isThrownBy(() -> userIdentityAuditLog.recordLogin(
                identity(), CLIENT_IP, USER_AGENT, true, false));
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
