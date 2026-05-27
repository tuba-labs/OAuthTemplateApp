package org.tubalabs.app.events;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tubalabs.app.events.db.EventLogDbo;
import org.tubalabs.app.events.db.EventLogRepository;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class EventLogServiceTest {

    private static final String EVENT_TYPE = "profile.updated";
    private static final UUID ACTOR_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SUBJECT_TYPE = "user_profile";
    private static final String SUBJECT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final String FIELD_NAME = "displayName";
    private static final Map<String, Object> DETAILS = Map.of("field", FIELD_NAME);

    private final EventLogRepository eventLogRepository = Mockito.mock(EventLogRepository.class);
    private final EventLogService eventLogService = new EventLogService(eventLogRepository);

    @Test
    void recordsEvent() {
        final EventLog eventLog = EventLog.builder()
                .eventType(EVENT_TYPE)
                .actorUserId(ACTOR_USER_ID)
                .subjectType(SUBJECT_TYPE)
                .subjectId(SUBJECT_ID)
                .clientIp(CLIENT_IP)
                .userAgent(USER_AGENT)
                .details(DETAILS)
                .build();
        final ArgumentCaptor<EventLogDbo> eventCaptor = ArgumentCaptor.forClass(EventLogDbo.class);

        final UUID eventId = eventLogService.record(eventLog);

        verify(eventLogRepository).insert(eventCaptor.capture());
        final EventLogDbo recordedEvent = eventCaptor.getValue();
        assertThat(recordedEvent.id()).isEqualTo(eventId);
        assertThat(recordedEvent.eventType()).isEqualTo(EVENT_TYPE);
        assertThat(recordedEvent.actorUserId()).isEqualTo(ACTOR_USER_ID);
        assertThat(recordedEvent.subjectType()).isEqualTo(SUBJECT_TYPE);
        assertThat(recordedEvent.subjectId()).isEqualTo(SUBJECT_ID);
        assertThat(recordedEvent.clientIp()).isEqualTo(CLIENT_IP);
        assertThat(recordedEvent.userAgent()).isEqualTo(USER_AGENT);
        assertThat(recordedEvent.details()).isEqualTo("{\"field\":\"displayName\"}");
    }
}
