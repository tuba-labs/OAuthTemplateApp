package org.tubalabs.app.users.identity.events;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.events.EventLog;
import org.tubalabs.app.events.EventType;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

import java.util.Map;
import java.util.Objects;

@Component
public class UserIdentityEventFactory {

    private static final String USER_IDENTITY_SUBJECT_TYPE = "user_identity";

    public EventLog login(@NonNull UserIdentityDbo identity,
                          @NonNull String clientIp,
                          @NonNull String userAgent,
                          boolean remembered,
                          boolean newUser) {
        return EventLog.builder()
                .eventType(EventType.USER_LOGIN.value())
                .actorUserId(identity.userId())
                .subjectType(USER_IDENTITY_SUBJECT_TYPE)
                .subjectId(identity.id().toString())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .details(Map.of(
                        "providerId", identity.providerId(),
                        "subject", identity.subject(),
                        "remembered", remembered,
                        "newUser", newUser))
                .build();
    }

    public EventLog signInMethodLinked(@NonNull UserIdentityDbo identity,
                                       @NonNull String clientIp,
                                       @NonNull String userAgent) {
        return EventLog.builder()
                .eventType(EventType.SIGN_IN_METHOD_LINKED.value())
                .actorUserId(identity.userId())
                .subjectType(USER_IDENTITY_SUBJECT_TYPE)
                .subjectId(identity.id().toString())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .details(Map.of(
                        "providerId", identity.providerId(),
                        "subject", identity.subject()))
                .build();
    }

    public EventLog logout(@NonNull UserIdentityDbo identity,
                           @NonNull String clientIp,
                           @NonNull String userAgent) {
        return EventLog.builder()
                .eventType(EventType.USER_LOGOUT.value())
                .actorUserId(identity.userId())
                .subjectType(USER_IDENTITY_SUBJECT_TYPE)
                .subjectId(identity.id().toString())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .details(Map.of(
                        "providerId", identity.providerId(),
                        "subject", identity.subject()))
                .build();
    }

    public EventLog unresolvedLogout(String principal,
                                     @NonNull String clientIp,
                                     @NonNull String userAgent) {
        return EventLog.builder()
                .eventType(EventType.USER_LOGOUT.value())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .details(Map.of("principal", Objects.requireNonNullElse(principal, "")))
                .build();
    }
}
