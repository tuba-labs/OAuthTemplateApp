package org.tubalabs.app.email.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.tubalabs.app.email.EmailMessage;
import org.tubalabs.app.email.EmailSenderPropertiesTestFactory;
import org.tubalabs.app.email.config.EmailSenderProperties;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDeliveryDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.DISPATCH_BATCH_SIZE;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.DISPATCH_FIXED_DELAY;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.DISPATCH_LOCK_DURATION;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.DISPATCH_SCHEDULER_POOL_SIZE;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.INITIAL_RETRY_DELAY;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.MAX_RETRY_DELAY;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.MESSAGE_ID_DOMAIN;
import static org.mockito.Mockito.verify;

class EmailOutboxServiceTest {

    private static final String RECIPIENT = "person@example.com";
    private static final String SUBJECT = "Test email";
    private static final String BODY = "This is a test email.";
    private static final EmailMessage MESSAGE = new EmailMessage(RECIPIENT, SUBJECT, BODY);
    private static final Instant NOW = Instant.parse("2026-05-29T12:00:00Z");
    private static final Duration LATEST_DELIVERY_DURATION = Duration.ofMinutes(20);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final EmailOutboxRepository emailOutboxRepository = Mockito.mock(EmailOutboxRepository.class);
    private final EmailSenderProperties emailSenderProperties = emailSenderProperties();
    private final EmailOutboxService emailOutboxService =
            new EmailOutboxService(emailOutboxRepository, emailSenderProperties, CLOCK);

    @Test
    void enqueuesEmailWithLatestDeliveryTime() {
        final ArgumentCaptor<EmailOutboxElementDbo> elementCaptor =
                ArgumentCaptor.forClass(EmailOutboxElementDbo.class);
        final ArgumentCaptor<EmailOutboxElementDeliveryDbo> deliveryCaptor =
                ArgumentCaptor.forClass(EmailOutboxElementDeliveryDbo.class);

        emailOutboxService.send(MESSAGE);

        verify(emailOutboxRepository).insert(elementCaptor.capture(), deliveryCaptor.capture());
        final EmailOutboxElementDbo capturedElement = elementCaptor.getValue();
        final EmailOutboxElementDeliveryDbo capturedDelivery = deliveryCaptor.getValue();
        assertThat(capturedElement.id()).isNotNull();
        assertThat(capturedElement.recipient()).isEqualTo(RECIPIENT);
        assertThat(capturedElement.subject()).isEqualTo(SUBJECT);
        assertThat(capturedElement.body()).isEqualTo(BODY);
        assertThat(capturedDelivery.emailOutboxElementId()).isEqualTo(capturedElement.id());
        assertThat(capturedDelivery.deliveryStatus()).isEqualTo(EmailOutboxStatus.PENDING.name());
        assertThat(capturedDelivery.attemptCount()).isZero();
        assertThat(capturedDelivery.nextAttemptAt()).isEqualTo(Timestamp.from(NOW));
        assertThat(capturedDelivery.latestDeliveryAt()).isEqualTo(Timestamp.from(NOW.plus(LATEST_DELIVERY_DURATION)));
        assertThat(capturedDelivery.deliveredAt()).isNull();
        assertThat(capturedDelivery.lastError()).isNull();
        assertThat(capturedDelivery.lockToken()).isNull();
        assertThat(capturedDelivery.lockedUntil()).isNull();
    }

    private static EmailSenderProperties emailSenderProperties() {
        return EmailSenderPropertiesTestFactory.emailSenderProperties(
                MESSAGE_ID_DOMAIN,
                LATEST_DELIVERY_DURATION,
                INITIAL_RETRY_DELAY,
                MAX_RETRY_DELAY,
                DISPATCH_LOCK_DURATION,
                DISPATCH_FIXED_DELAY,
                DISPATCH_BATCH_SIZE,
                DISPATCH_SCHEDULER_POOL_SIZE);
    }
}
