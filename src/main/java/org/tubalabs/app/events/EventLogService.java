package org.tubalabs.app.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.events.db.EventLogDbo;
import org.tubalabs.app.events.db.EventLogRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UUID record(@NonNull EventLog eventLog) {
        final UUID id = UUID.randomUUID();
        eventLogRepository.insert(EventLogDbo.builder()
                .id(id)
                .eventType(eventLog.eventType())
                .actorUserId(eventLog.actorUserId())
                .subjectType(eventLog.subjectType())
                .subjectId(eventLog.subjectId())
                .clientIp(eventLog.clientIp())
                .userAgent(eventLog.userAgent())
                .details(detailsJson(eventLog))
                .build());
        return id;
    }

    private String detailsJson(@NonNull EventLog eventLog) {
        try {
            return objectMapper.writeValueAsString(eventLog.details());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Event details must be JSON serializable", exception);
        }
    }
}
