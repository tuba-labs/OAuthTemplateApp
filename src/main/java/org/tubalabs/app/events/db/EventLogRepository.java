package org.tubalabs.app.events.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventLogRepository {

    private final JdbcClient jdbcClient;

    public void insert(@NonNull EventLogDbo eventLog) {
        jdbcClient.sql("""
                        INSERT INTO event_log (
                            id,
                            event_type,
                            actor_user_id,
                            subject_type,
                            subject_id,
                            client_ip,
                            user_agent,
                            details
                        )
                        VALUES (
                            :id,
                            :event_type,
                            :actor_user_id,
                            :subject_type,
                            :subject_id,
                            :client_ip,
                            :user_agent,
                            CAST(:details AS jsonb)
                        )
                """)
                .param("id", eventLog.id())
                .param("event_type", eventLog.eventType())
                .param("actor_user_id", eventLog.actorUserId())
                .param("subject_type", eventLog.subjectType())
                .param("subject_id", eventLog.subjectId())
                .param("client_ip", eventLog.clientIp())
                .param("user_agent", eventLog.userAgent())
                .param("details", eventLog.details())
                .update();
    }
}
