package org.tubalabs.app.email.outbox;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.email.EmailMessage;
import org.tubalabs.app.email.EmailSender;
import org.tubalabs.app.email.config.EmailSenderProperties;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDeliveryDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailOutboxService implements EmailSender {

    private final @NonNull EmailOutboxRepository emailOutboxRepository;
    private final @NonNull EmailSenderProperties emailSenderProperties;
    private final @NonNull Clock clock;

    @Override
    @Transactional
    public void send(@NonNull EmailMessage message) {
        addEmail(message);
    }

    @Transactional
    public void addEmail(@NonNull EmailMessage message) {
        final Instant now = clock.instant();
        final UUID emailId = UUID.randomUUID();
        final Instant latestDeliveryTime = now.plus(emailSenderProperties.latestDeliveryDuration());
        final EmailOutboxElementDbo emailOutboxElement = EmailOutboxElementDbo.builder()
                .id(emailId)
                .recipient(message.recipient())
                .subject(message.subject())
                .body(message.body())
                .build();
        final EmailOutboxElementDeliveryDbo delivery = EmailOutboxElementDeliveryDbo.builder()
                .emailOutboxElementId(emailId)
                .deliveryStatus(EmailOutboxStatus.PENDING.name())
                .attemptCount(0)
                .nextAttemptAt(Timestamp.from(now))
                .latestDeliveryAt(Timestamp.from(latestDeliveryTime))
                .build();
        emailOutboxRepository.insert(emailOutboxElement, delivery);
    }
}
