package org.tubalabs.app.users.identity.logins;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tubalabs.app.events.EventLog;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserIdentityAuditLog {

    private final @NonNull Clock clock;
    private final @NonNull UserIdentityAuditWriter userIdentityAuditWriter;
    private final @NonNull UserIdentityEventFactory userIdentityEventFactory;

    public void recordLogin(@NonNull UserIdentityDbo identity,
                            @NonNull String clientIp,
                            @NonNull String userAgent,
                            boolean remembered,
                            boolean newUser) {
        recordLoginRow(identity, clientIp, userAgent);
        recordEvent(userIdentityEventFactory.login(identity, clientIp, userAgent, remembered, newUser));
    }

    public void recordLoginRow(@NonNull UserIdentityDbo identity,
                               @NonNull String clientIp,
                               @NonNull String userAgent) {
        runSafely("login row", () -> userIdentityAuditWriter.insertLogin(newLogin(identity, clientIp, userAgent)));
    }

    public void recordSignInMethodLinked(@NonNull UserIdentityDbo identity,
                                         @NonNull String clientIp,
                                         @NonNull String userAgent) {
        recordEvent(userIdentityEventFactory.signInMethodLinked(identity, clientIp, userAgent));
    }

    public void recordEvent(@NonNull EventLog eventLog) {
        runSafely("event log", () -> userIdentityAuditWriter.insertEvent(eventLog));
    }

    private UserLoginDbo newLogin(@NonNull UserIdentityDbo identity,
                                  @NonNull String clientIp,
                                  @NonNull String userAgent) {
        return UserLoginDbo.builder()
                .id(UUID.randomUUID())
                .userId(identity.userId())
                .loginTime(Timestamp.from(clock.instant()))
                .providerId(identity.providerId())
                .subject(identity.subject())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
    }

    private void runSafely(@NonNull String description, @NonNull Runnable auditWrite) {
        try {
            auditWrite.run();
        } catch (RuntimeException exception) {
            log.warn("Could not write {} audit: {}", description, exception.toString());
        }
    }
}
