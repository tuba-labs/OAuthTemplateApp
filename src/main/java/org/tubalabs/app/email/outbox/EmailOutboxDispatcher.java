package org.tubalabs.app.email.outbox;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.tubalabs.app.email.EmailMessage;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class EmailOutboxDispatcher {

    private final @NonNull EmailOutboxRepository emailOutboxRepository;
    private final @NonNull EmailTransport emailTransport;
    private final @NonNull EmailSenderProperties emailSenderProperties;
    private final @NonNull Clock clock;

    public int dispatchDueEmails() {
        int dispatchedCount = 0;
        for (int emailIndex = 0; emailIndex < emailSenderProperties.dispatchBatchSize(); emailIndex++) {
            final Optional<EmailOutboxElementWithDelivery> claimedEmail = claimNextDueEmail();
            if (claimedEmail.isEmpty()) {
                return dispatchedCount;
            }
            dispatch(claimedEmail.orElseThrow());
            dispatchedCount++;
        }
        return dispatchedCount;
    }

    private Optional<EmailOutboxElementWithDelivery> claimNextDueEmail() {
        final Instant now = clock.instant();
        return emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(now),
                Timestamp.from(now.plus(emailSenderProperties.dispatchLockDuration())),
                UUID.randomUUID());
    }

    private void dispatch(@NonNull EmailOutboxElementWithDelivery emailOutboxItem) {
        final EmailOutboxElementDbo element = emailOutboxItem.element();
        final EmailMessage message = new EmailMessage(element.recipient(), element.subject(), element.body());
        try {
            emailTransport.deliver(element.id(), message);
        } catch (EmailDeliveryException exception) {
            recordFailedAttempt(emailOutboxItem, exception);
            return;
        }
        recordDelivered(emailOutboxItem);
    }

    private void recordDelivered(@NonNull EmailOutboxElementWithDelivery emailOutboxItem) {
        final int updatedRows = emailOutboxRepository.markSent(
                emailOutboxItem.element().id(),
                lockToken(emailOutboxItem.delivery()),
                Timestamp.from(clock.instant()));
        if (updatedRows == 0) {
            log.warn("Email outbox item {} was delivered but no longer belongs to this dispatcher",
                    emailOutboxItem.element().id());
        }
    }

    private void recordFailedAttempt(@NonNull EmailOutboxElementWithDelivery emailOutboxItem,
                                     @NonNull RuntimeException exception) {
        final EmailOutboxElementDeliveryDbo delivery = emailOutboxItem.delivery();
        final int attemptCount = delivery.attemptCount() + 1;
        final Instant now = clock.instant();
        final Instant nextAttemptAt = now.plus(retryDelay(attemptCount));
        final Instant latestDeliveryAt = delivery.latestDeliveryAt().toInstant();
        final String lastError = lastError(exception);

        if (now.isAfter(latestDeliveryAt) || nextAttemptAt.isAfter(latestDeliveryAt)) {
            final int updatedRows = emailOutboxRepository.markFailed(
                    emailOutboxItem.element().id(),
                    lockToken(delivery),
                    attemptCount,
                    lastError);
            if (updatedRows == 0) {
                log.warn("Email outbox item {} failed permanently but no longer belongs to this dispatcher",
                        emailOutboxItem.element().id());
            }
            log.warn(
                    "Email outbox item {} failed permanently after {} attempts",
                    emailOutboxItem.element().id(),
                    attemptCount,
                    exception);
            return;
        }

        final int updatedRows = emailOutboxRepository.markRetry(
                emailOutboxItem.element().id(),
                lockToken(delivery),
                attemptCount,
                Timestamp.from(nextAttemptAt),
                lastError);
        if (updatedRows == 0) {
            log.warn("Email outbox item {} retry could not be recorded because the claim changed",
                    emailOutboxItem.element().id());
        }
        log.warn(
                "Email outbox item {} failed attempt {}; retry at {}",
                emailOutboxItem.element().id(),
                attemptCount,
                nextAttemptAt,
                exception);
    }

    private Duration retryDelay(int attemptCount) {
        final int cappedAttemptCount = Math.min(attemptCount - 1, 10);
        final long multiplier = 1L << cappedAttemptCount;
        final Duration retryDelay = emailSenderProperties.initialRetryDelay().multipliedBy(multiplier);
        if (retryDelay.compareTo(emailSenderProperties.maxRetryDelay()) > 0) {
            return emailSenderProperties.maxRetryDelay();
        }
        return retryDelay;
    }

    private String lastError(@NonNull RuntimeException exception) {
        final String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getName();
        }
        return message;
    }

    private UUID lockToken(@NonNull EmailOutboxElementDeliveryDbo delivery) {
        final UUID lockToken = delivery.lockToken();
        if (lockToken == null) {
            throw new IllegalStateException(
                    "Claimed email outbox item has no lock token: " + delivery.emailOutboxElementId());
        }
        return lockToken;
    }
}
