package org.tubalabs.app.email.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDeliveryDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementWithDelivery;
import org.tubalabs.app.email.outbox.db.EmailOutboxRepository;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        SqlColumnNameResolver.class,
        SqlRecordIntrospector.class,
        EmailOutboxRepository.class
})
class EmailOutboxRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID EMAIL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID EXPIRED_EMAIL_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID LOCKED_EMAIL_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID FAILED_EMAIL_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID LOCK_TOKEN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_LOCK_TOKEN = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String RECIPIENT = "person@example.com";
    private static final String SUBJECT = "Test email";
    private static final String BODY = "This is a test email.";
    private static final String RETRY_ERROR = "SMTP unavailable";
    private static final Instant NOW = Instant.parse("2026-05-29T12:00:00Z");
    private static final Instant NEXT_RETRY = Instant.parse("2026-05-29T12:00:10Z");
    private static final Instant LOCKED_UNTIL = Instant.parse("2026-05-29T12:02:00Z");
    private static final Instant NEXT_RETRY_LOCKED_UNTIL = Instant.parse("2026-05-29T12:02:10Z");
    private static final Instant SHORT_LOCKED_UNTIL = Instant.parse("2026-05-29T12:00:05Z");
    private static final Instant AFTER_SHORT_LOCK = Instant.parse("2026-05-29T12:00:06Z");
    private static final Instant LATEST_DELIVERY_TIME = Instant.parse("2026-05-29T12:15:00Z");
    private static final Instant PAST_NEXT_ATTEMPT = Instant.parse("2026-05-29T11:58:00Z");
    private static final Instant PAST_LATEST_DELIVERY_TIME = Instant.parse("2026-05-29T11:59:00Z");
    private static final Instant OLD_MODIFIED_TIME = Instant.parse("2026-05-28T12:00:00Z");

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void insertsAndClaimsDueEmail() {
        final EmailOutboxElementWithDelivery emailOutboxItem = pendingEmail(EMAIL_ID, LATEST_DELIVERY_TIME);
        final EmailOutboxElementWithDelivery expectedClaimedEmail =
                claimedEmail(emailOutboxItem, LOCK_TOKEN, LOCKED_UNTIL);

        assertThat(insert(emailOutboxItem)).isEqualTo(emailOutboxItem);

        assertThat(emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                LOCK_TOKEN)).contains(expectedClaimedEmail);
        assertThat(findById(EMAIL_ID)).contains(expectedClaimedEmail);
    }

    @Test
    void recordsRetryAndDelivery() {
        insert(pendingEmail(EMAIL_ID, LATEST_DELIVERY_TIME));
        emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                LOCK_TOKEN);

        assertThat(emailOutboxRepository.markRetry(
                EMAIL_ID,
                LOCK_TOKEN,
                1,
                Timestamp.from(NEXT_RETRY),
                RETRY_ERROR)).isOne();
        final EmailOutboxElementDeliveryDbo retryDelivery = findById(EMAIL_ID).orElseThrow().delivery();
        assertThat(retryDelivery.deliveryStatus()).isEqualTo(EmailOutboxStatus.PENDING.name());
        assertThat(retryDelivery.attemptCount()).isEqualTo(1);
        assertThat(retryDelivery.nextAttemptAt()).isEqualTo(Timestamp.from(NEXT_RETRY));
        assertThat(retryDelivery.lastError()).isEqualTo(RETRY_ERROR);
        assertThat(retryDelivery.lockToken()).isNull();
        assertThat(retryDelivery.lockedUntil()).isNull();

        emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NEXT_RETRY),
                Timestamp.from(NEXT_RETRY_LOCKED_UNTIL),
                LOCK_TOKEN);

        assertThat(emailOutboxRepository.markSent(EMAIL_ID, LOCK_TOKEN, Timestamp.from(NEXT_RETRY))).isOne();
        final EmailOutboxElementDeliveryDbo sentDelivery = findById(EMAIL_ID).orElseThrow().delivery();
        assertThat(sentDelivery.deliveryStatus()).isEqualTo(EmailOutboxStatus.SENT.name());
        assertThat(sentDelivery.deliveredAt()).isEqualTo(Timestamp.from(NEXT_RETRY));
        assertThat(sentDelivery.lastError()).isNull();
        assertThat(sentDelivery.lockToken()).isNull();
        assertThat(sentDelivery.lockedUntil()).isNull();
    }

    @Test
    void doesNotClaimEmailsPastLatestDeliveryTime() {
        final EmailOutboxElementWithDelivery pastDueEmail = pendingEmail(
                EXPIRED_EMAIL_ID,
                PAST_NEXT_ATTEMPT,
                PAST_LATEST_DELIVERY_TIME);
        insert(pastDueEmail);

        assertThat(findById(EXPIRED_EMAIL_ID)).contains(pastDueEmail);
        assertThat(emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                LOCK_TOKEN)).isEmpty();
    }

    @Test
    void doesNotClaimEmailWithActiveLease() {
        final EmailOutboxElementWithDelivery dueEmail = pendingEmail(LOCKED_EMAIL_ID, LATEST_DELIVERY_TIME);
        insert(dueEmail);

        assertThat(emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                LOCK_TOKEN)).isPresent();

        assertThat(emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                OTHER_LOCK_TOKEN)).isEmpty();
    }

    @Test
    void claimsEmailAfterLeaseExpires() {
        final EmailOutboxElementWithDelivery dueEmail = pendingEmail(LOCKED_EMAIL_ID, LATEST_DELIVERY_TIME);
        final EmailOutboxElementWithDelivery expectedReclaimedEmail =
                claimedEmail(dueEmail, OTHER_LOCK_TOKEN, LOCKED_UNTIL);
        insert(dueEmail);

        emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(SHORT_LOCKED_UNTIL),
                LOCK_TOKEN);

        assertThat(emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(AFTER_SHORT_LOCK),
                Timestamp.from(LOCKED_UNTIL),
                OTHER_LOCK_TOKEN)).contains(expectedReclaimedEmail);
    }

    @Test
    void doesNotMarkSentWithWrongLockToken() {
        final EmailOutboxElementWithDelivery dueEmail = pendingEmail(EMAIL_ID, LATEST_DELIVERY_TIME);
        final EmailOutboxElementWithDelivery claimedEmail = claimedEmail(dueEmail, LOCK_TOKEN, LOCKED_UNTIL);
        insert(dueEmail);
        emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                LOCK_TOKEN);

        assertThat(emailOutboxRepository.markSent(EMAIL_ID, OTHER_LOCK_TOKEN, Timestamp.from(NOW))).isZero();
        assertThat(findById(EMAIL_ID)).contains(claimedEmail);
    }

    @Test
    void updatesModifiedWhenClaimingAndRecordingOutcomes() {
        insert(pendingEmail(EMAIL_ID, LATEST_DELIVERY_TIME));

        setDeliveryModified(EMAIL_ID, OLD_MODIFIED_TIME);
        emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                LOCK_TOKEN);
        assertDeliveryModifiedAfterOldTime(EMAIL_ID);

        setDeliveryModified(EMAIL_ID, OLD_MODIFIED_TIME);
        emailOutboxRepository.markRetry(EMAIL_ID, LOCK_TOKEN, 1, Timestamp.from(NEXT_RETRY), RETRY_ERROR);
        assertDeliveryModifiedAfterOldTime(EMAIL_ID);

        emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NEXT_RETRY),
                Timestamp.from(NEXT_RETRY_LOCKED_UNTIL),
                LOCK_TOKEN);
        setDeliveryModified(EMAIL_ID, OLD_MODIFIED_TIME);
        emailOutboxRepository.markSent(EMAIL_ID, LOCK_TOKEN, Timestamp.from(NEXT_RETRY));
        assertDeliveryModifiedAfterOldTime(EMAIL_ID);

        insert(pendingEmail(FAILED_EMAIL_ID, LATEST_DELIVERY_TIME));
        emailOutboxRepository.claimNextDueForDelivery(
                Timestamp.from(NOW),
                Timestamp.from(LOCKED_UNTIL),
                LOCK_TOKEN);
        setDeliveryModified(FAILED_EMAIL_ID, OLD_MODIFIED_TIME);
        emailOutboxRepository.markFailed(FAILED_EMAIL_ID, LOCK_TOKEN, 1, RETRY_ERROR);
        assertDeliveryModifiedAfterOldTime(FAILED_EMAIL_ID);
    }

    private EmailOutboxElementWithDelivery insert(EmailOutboxElementWithDelivery emailOutboxItem) {
        emailOutboxRepository.insert(emailOutboxItem.element(), emailOutboxItem.delivery());
        return emailOutboxItem;
    }

    private Optional<EmailOutboxElementWithDelivery> findById(UUID id) {
        return jdbcClient.sql("""
                        SELECT
                            element.id AS element_id,
                            element.recipient AS element_recipient,
                            element.subject AS element_subject,
                            element.body AS element_body,
                            delivery.email_outbox_element_id AS delivery_email_outbox_element_id,
                            delivery.delivery_status AS delivery_delivery_status,
                            delivery.attempt_count AS delivery_attempt_count,
                            delivery.next_attempt_at AS delivery_next_attempt_at,
                            delivery.latest_delivery_at AS delivery_latest_delivery_at,
                            delivery.delivered_at AS delivery_delivered_at,
                            delivery.last_error AS delivery_last_error,
                            delivery.lock_token AS delivery_lock_token,
                            delivery.locked_until AS delivery_locked_until
                        FROM email_outbox_element AS element
                        JOIN email_outbox_element_delivery AS delivery
                          ON delivery.email_outbox_element_id = element.id
                        WHERE element.id = :id
                """)
                .param("id", id)
                .query((resultSet, rowNumber) -> new EmailOutboxElementWithDelivery(
                        new EmailOutboxElementDbo(
                                resultSet.getObject("element_id", UUID.class),
                                resultSet.getString("element_recipient"),
                                resultSet.getString("element_subject"),
                                resultSet.getString("element_body")),
                        new EmailOutboxElementDeliveryDbo(
                                resultSet.getObject("delivery_email_outbox_element_id", UUID.class),
                                resultSet.getString("delivery_delivery_status"),
                                resultSet.getInt("delivery_attempt_count"),
                                resultSet.getTimestamp("delivery_next_attempt_at"),
                                resultSet.getTimestamp("delivery_latest_delivery_at"),
                                resultSet.getTimestamp("delivery_delivered_at"),
                                resultSet.getString("delivery_last_error"),
                                resultSet.getObject("delivery_lock_token", UUID.class),
                                resultSet.getTimestamp("delivery_locked_until"))))
                .optional();
    }

    private static EmailOutboxElementWithDelivery pendingEmail(UUID id, Instant latestDeliveryTime) {
        return pendingEmail(id, NOW, latestDeliveryTime);
    }

    private static EmailOutboxElementWithDelivery pendingEmail(
            UUID id, Instant nextAttemptAt, Instant latestDeliveryTime) {
        final EmailOutboxElementDbo element = EmailOutboxElementDbo.builder()
                .id(id)
                .recipient(RECIPIENT)
                .subject(SUBJECT)
                .body(BODY)
                .build();
        final EmailOutboxElementDeliveryDbo delivery = EmailOutboxElementDeliveryDbo.builder()
                .emailOutboxElementId(id)
                .deliveryStatus(EmailOutboxStatus.PENDING.name())
                .attemptCount(0)
                .nextAttemptAt(Timestamp.from(nextAttemptAt))
                .latestDeliveryAt(Timestamp.from(latestDeliveryTime))
                .build();
        return new EmailOutboxElementWithDelivery(element, delivery);
    }

    private void setDeliveryModified(UUID id, Instant modifiedAt) {
        jdbcClient.sql("""
                        UPDATE email_outbox_element_delivery
                        SET modified = :modified
                        WHERE email_outbox_element_id = :id
                """)
                .param("id", id)
                .param("modified", Timestamp.from(modifiedAt))
                .update();
    }

    private void assertDeliveryModifiedAfterOldTime(UUID id) {
        assertThat(deliveryModifiedAt(id).toInstant()).isAfter(OLD_MODIFIED_TIME);
    }

    private Timestamp deliveryModifiedAt(UUID id) {
        return jdbcClient.sql("""
                        SELECT modified
                        FROM email_outbox_element_delivery
                        WHERE email_outbox_element_id = :id
                """)
                .param("id", id)
                .query(Timestamp.class)
                .single();
    }

    private static EmailOutboxElementWithDelivery claimedEmail(EmailOutboxElementWithDelivery emailOutboxItem,
                                                               UUID lockToken,
                                                               Instant lockedUntil) {
        final EmailOutboxElementDeliveryDbo claimedDelivery = emailOutboxItem.delivery().toBuilder()
                .lockToken(lockToken)
                .lockedUntil(Timestamp.from(lockedUntil))
                .build();
        return new EmailOutboxElementWithDelivery(emailOutboxItem.element(), claimedDelivery);
    }
}
