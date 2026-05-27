package org.tubalabs.app.events.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EventLogRepository.class)
class EventLogRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID EVENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ACTOR_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EVENT_TYPE = "profile.updated";
    private static final String SUBJECT_TYPE = "user_profile";
    private static final String SUBJECT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final String DETAILS = "{\"field\":\"displayName\"}";
    private static final String DETAILS_FIELD = "displayName";

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void insertsEventWithoutExistingActorUser() {
        final EventLogDbo eventLog = EventLogDbo.builder()
                .id(EVENT_ID)
                .eventType(EVENT_TYPE)
                .actorUserId(ACTOR_USER_ID)
                .subjectType(SUBJECT_TYPE)
                .subjectId(SUBJECT_ID)
                .clientIp(CLIENT_IP)
                .userAgent(USER_AGENT)
                .details(DETAILS)
                .build();

        eventLogRepository.insert(eventLog);

        assertThat(eventCount(EVENT_ID)).isEqualTo(1);
        assertThat(detailsField(EVENT_ID)).isEqualTo(DETAILS_FIELD);
    }

    private int eventCount(UUID eventId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM event_log WHERE id = :id")
                .param("id", eventId)
                .query(Integer.class)
                .single();
    }

    private String detailsField(UUID eventId) {
        return jdbcClient.sql("SELECT details ->> 'field' FROM event_log WHERE id = :id")
                .param("id", eventId)
                .query(String.class)
                .single();
    }
}
