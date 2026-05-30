package org.tubalabs.app.email.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tubalabs.app.email.EmailMessage;
import org.tubalabs.app.email.EmailSenderPropertiesTestFactory;
import org.tubalabs.app.email.EmailTransport;
import org.tubalabs.app.email.config.EmailSenderProperties;
import org.tubalabs.app.email.exceptions.EmailDeliveryException;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDeliveryDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementWithDelivery;
import org.tubalabs.app.email.outbox.db.EmailOutboxRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.DISPATCH_FIXED_DELAY;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.DISPATCH_SCHEDULER_POOL_SIZE;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.LATEST_DELIVERY_DURATION;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.MESSAGE_ID_DOMAIN;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailOutboxDispatcherTest {

    private static final UUID EMAIL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID LOCK_TOKEN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String RECIPIENT = "person@example.com";
    private static final String SUBJECT = "Test email";
    private static final String BODY = "This is a test email.";
    private static final String FAILURE_MESSAGE = "SMTP unavailable";
    private static final Instant NOW = Instant.parse("2026-05-29T12:00:00Z");
    private static final Instant LATEST_DELIVERY_TIME = NOW.plus(Duration.ofMinutes(10));
    private static final Instant SOON_LATEST_DELIVERY_TIME = NOW.plus(Duration.ofSeconds(5));
    private static final Instant FIRST_RETRY_TIME = NOW.plus(Duration.ofSeconds(10));
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(10);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(2);
    private static final Instant LOCKED_UNTIL = NOW.plus(LOCK_DURATION);
    private static final int BATCH_SIZE = 25;
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final EmailMessage MESSAGE = new EmailMessage(RECIPIENT, SUBJECT, BODY);

    private final EmailOutboxRepository emailOutboxRepository = Mockito.mock(EmailOutboxRepository.class);
    private final EmailTransport emailTransport = Mockito.mock(EmailTransport.class);
    private final EmailSenderProperties emailSenderProperties = emailSenderProperties();
    private final EmailOutboxDispatcher emailOutboxDispatcher =
            new EmailOutboxDispatcher(emailOutboxRepository, emailTransport, emailSenderProperties, CLOCK);

    @Test
    void sendsDueEmailAndMarksItDelivered() {
        final EmailOutboxElementWithDelivery dueEmail = dueEmail(LATEST_DELIVERY_TIME);
        whenClaimingDueEmail(dueEmail);
        when(emailOutboxRepository.markSent(EMAIL_ID, LOCK_TOKEN, Timestamp.from(NOW))).thenReturn(1);

        emailOutboxDispatcher.dispatchDueEmails();

        verify(emailTransport).deliver(EMAIL_ID, MESSAGE);
        verify(emailOutboxRepository).markSent(EMAIL_ID, LOCK_TOKEN, Timestamp.from(NOW));
    }

    @Test
    void doesNotRecordRetryWhenDeliverySucceededButMarkSentFails() {
        final EmailOutboxElementWithDelivery dueEmail = dueEmail(LATEST_DELIVERY_TIME);
        final RuntimeException databaseFailure = new RuntimeException("database unavailable");
        whenClaimingDueEmail(dueEmail);
        Mockito.doThrow(databaseFailure)
                .when(emailOutboxRepository)
                .markSent(EMAIL_ID, LOCK_TOKEN, Timestamp.from(NOW));

        assertThatThrownBy(emailOutboxDispatcher::dispatchDueEmails)
                .isSameAs(databaseFailure);

        verify(emailTransport).deliver(EMAIL_ID, MESSAGE);
        verify(emailOutboxRepository, never())
                .markRetry(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
        verify(emailOutboxRepository, never()).markFailed(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void recordsRetryWhenDeliveryFailsBeforeLatestDeliveryTime() {
        final EmailOutboxElementWithDelivery dueEmail = dueEmail(LATEST_DELIVERY_TIME);
        whenClaimingDueEmail(dueEmail);
        Mockito.doThrow(new EmailDeliveryException(FAILURE_MESSAGE, new RuntimeException("network")))
                .when(emailTransport)
                .deliver(EMAIL_ID, MESSAGE);
        when(emailOutboxRepository.markRetry(
                EMAIL_ID,
                LOCK_TOKEN,
                1,
                Timestamp.from(FIRST_RETRY_TIME),
                FAILURE_MESSAGE)).thenReturn(1);

        emailOutboxDispatcher.dispatchDueEmails();

        verify(emailOutboxRepository).markRetry(
                EMAIL_ID,
                LOCK_TOKEN,
                1,
                Timestamp.from(FIRST_RETRY_TIME),
                FAILURE_MESSAGE);
        verify(emailOutboxRepository, never()).markSent(Mockito.any(), Mockito.any(), Mockito.any());
        verify(emailOutboxRepository, never()).markFailed(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void doesNotRecordRetryForUnexpectedTransportRuntimeException() {
        final EmailOutboxElementWithDelivery dueEmail = dueEmail(LATEST_DELIVERY_TIME);
        final RuntimeException unexpectedException = new IllegalStateException("invalid sender configuration");
        whenClaimingDueEmail(dueEmail);
        Mockito.doThrow(unexpectedException)
                .when(emailTransport)
                .deliver(EMAIL_ID, MESSAGE);

        assertThatThrownBy(emailOutboxDispatcher::dispatchDueEmails)
                .isSameAs(unexpectedException);

        verify(emailOutboxRepository, never()).markSent(Mockito.any(), Mockito.any(), Mockito.any());
        verify(emailOutboxRepository, never())
                .markRetry(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
        verify(emailOutboxRepository, never()).markFailed(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void marksEmailFailedWhenNextRetryWouldMissLatestDeliveryTime() {
        final EmailOutboxElementWithDelivery dueEmail = dueEmail(SOON_LATEST_DELIVERY_TIME);
        whenClaimingDueEmail(dueEmail);
        Mockito.doThrow(new EmailDeliveryException(FAILURE_MESSAGE, new RuntimeException("network")))
                .when(emailTransport)
                .deliver(EMAIL_ID, MESSAGE);
        when(emailOutboxRepository.markFailed(EMAIL_ID, LOCK_TOKEN, 1, FAILURE_MESSAGE)).thenReturn(1);

        emailOutboxDispatcher.dispatchDueEmails();

        verify(emailOutboxRepository).markFailed(EMAIL_ID, LOCK_TOKEN, 1, FAILURE_MESSAGE);
        verify(emailOutboxRepository, never())
                .markRetry(Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
    }

    private void whenClaimingDueEmail(EmailOutboxElementWithDelivery dueEmail) {
        when(emailOutboxRepository.claimNextDueForDelivery(
                Mockito.eq(Timestamp.from(NOW)),
                Mockito.eq(Timestamp.from(LOCKED_UNTIL)),
                Mockito.any(UUID.class)))
                .thenReturn(Optional.of(dueEmail), Optional.empty());
    }

    private static EmailSenderProperties emailSenderProperties() {
        return EmailSenderPropertiesTestFactory.emailSenderProperties(
                MESSAGE_ID_DOMAIN,
                LATEST_DELIVERY_DURATION,
                INITIAL_RETRY_DELAY,
                MAX_RETRY_DELAY,
                LOCK_DURATION,
                DISPATCH_FIXED_DELAY,
                BATCH_SIZE,
                DISPATCH_SCHEDULER_POOL_SIZE);
    }

    private static EmailOutboxElementWithDelivery dueEmail(Instant latestDeliveryTime) {
        final EmailOutboxElementDbo element = EmailOutboxElementDbo.builder()
                .id(EMAIL_ID)
                .recipient(RECIPIENT)
                .subject(SUBJECT)
                .body(BODY)
                .build();
        final EmailOutboxElementDeliveryDbo delivery = EmailOutboxElementDeliveryDbo.builder()
                .emailOutboxElementId(EMAIL_ID)
                .deliveryStatus(EmailOutboxStatus.PENDING.name())
                .attemptCount(0)
                .nextAttemptAt(Timestamp.from(NOW))
                .latestDeliveryAt(Timestamp.from(latestDeliveryTime))
                .lockToken(LOCK_TOKEN)
                .lockedUntil(Timestamp.from(LOCKED_UNTIL))
                .build();
        return new EmailOutboxElementWithDelivery(element, delivery);
    }
}
