package org.tubalabs.app.email.outbox.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.email.outbox.EmailOutboxStatus;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmailOutboxRepository {

    private static final String ELEMENT_TABLE_NAME = "email_outbox_element";
    private static final String DELIVERY_TABLE_NAME = "email_outbox_element_delivery";
    private static final String ELEMENT_ALIAS = "element";
    private static final String DELIVERY_ALIAS = "delivery";
    private static final String DUE_DELIVERY_ALIAS = "due_delivery";
    private static final String CLAIMED_DELIVERY_ALIAS = "claimed_delivery";
    private static final String ELEMENT_RESULT_PREFIX = "element";
    private static final String DELIVERY_RESULT_PREFIX = "delivery";
    private static final String ID_FIELD = "id";
    private static final String EMAIL_OUTBOX_ELEMENT_ID_FIELD = "emailOutboxElementId";
    private static final String DELIVERY_STATUS_FIELD = "deliveryStatus";
    private static final String ATTEMPT_COUNT_FIELD = "attemptCount";
    private static final String NEXT_ATTEMPT_AT_FIELD = "nextAttemptAt";
    private static final String LATEST_DELIVERY_AT_FIELD = "latestDeliveryAt";
    private static final String DELIVERED_AT_FIELD = "deliveredAt";
    private static final String LAST_ERROR_FIELD = "lastError";
    private static final String LOCK_TOKEN_FIELD = "lockToken";
    private static final String LOCKED_UNTIL_FIELD = "lockedUntil";
    private static final String MODIFIED_FIELD = "modified";
    private static final String CREATED_FIELD = "created";
    private static final String RECIPIENT_FIELD = "recipient";
    private static final String SUBJECT_FIELD = "subject";
    private static final String BODY_FIELD = "body";

    private final JdbcClient jdbcClient;
    private final SqlRecordIntrospector sqlRecordIntrospector;

    @Transactional
    public void insert(@NonNull EmailOutboxElementDbo emailOutboxElement,
                       @NonNull EmailOutboxElementDeliveryDbo delivery) {
        if (!emailOutboxElement.id().equals(delivery.emailOutboxElementId())) {
            throw new IllegalArgumentException("Outbox element id and delivery element id must match");
        }

        insertElement(emailOutboxElement);
        insertDelivery(delivery);
    }

    public Optional<EmailOutboxElementWithDelivery> claimNextDueForDelivery(@NonNull Timestamp now,
                                                                            @NonNull Timestamp lockedUntil,
                                                                            @NonNull UUID lockToken) {
        return jdbcClient.sql("""
                        WITH due_delivery AS (
                            SELECT %s
                            FROM %s AS %s
                            WHERE %s = :delivery_status
                              AND %s <= :now
                              AND %s >= :now
                              AND (%s IS NULL OR %s <= :now)
                            ORDER BY %s, %s, %s
                            LIMIT 1
                            FOR UPDATE SKIP LOCKED
                        ),
                        claimed_delivery AS (
                            UPDATE %s AS %s
                            SET %s = :lock_token,
                                %s = :locked_until,
                                %s = CURRENT_TIMESTAMP
                            FROM due_delivery
                            WHERE %s = %s
                            RETURNING
                                %s
                        )
                        SELECT
                            %s
                        FROM claimed_delivery
                        JOIN %s AS %s
                          ON %s = %s
                """.formatted(
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, EMAIL_OUTBOX_ELEMENT_ID_FIELD),
                        DELIVERY_TABLE_NAME,
                        DELIVERY_ALIAS,
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, DELIVERY_STATUS_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, NEXT_ATTEMPT_AT_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, LATEST_DELIVERY_AT_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, LOCKED_UNTIL_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, LOCKED_UNTIL_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, NEXT_ATTEMPT_AT_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, CREATED_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, EMAIL_OUTBOX_ELEMENT_ID_FIELD),
                        DELIVERY_TABLE_NAME,
                        DELIVERY_ALIAS,
                        deliveryColumn(LOCK_TOKEN_FIELD),
                        deliveryColumn(LOCKED_UNTIL_FIELD),
                        deliveryColumn(MODIFIED_FIELD),
                        qualifiedDeliveryColumn(DELIVERY_ALIAS, EMAIL_OUTBOX_ELEMENT_ID_FIELD),
                        qualifiedDeliveryColumn(DUE_DELIVERY_ALIAS, EMAIL_OUTBOX_ELEMENT_ID_FIELD),
                        returningDeliveryColumns(),
                        selectedElementWithDeliveryColumns(),
                        ELEMENT_TABLE_NAME,
                        ELEMENT_ALIAS,
                        qualifiedElementColumn(ELEMENT_ALIAS, ID_FIELD),
                        qualifiedDeliveryColumn(CLAIMED_DELIVERY_ALIAS, EMAIL_OUTBOX_ELEMENT_ID_FIELD)))
                .param("delivery_status", EmailOutboxStatus.PENDING.name())
                .param("now", now)
                .param("lock_token", lockToken)
                .param("locked_until", lockedUntil)
                .query(this::mapElementWithDelivery)
                .optional();
    }

    public int markSent(@NonNull UUID id, @NonNull UUID lockToken, @NonNull Timestamp deliveredAt) {
        return jdbcClient.sql("""
                        UPDATE %s
                        SET %s = :delivery_status,
                            %s = :delivered_at,
                            %s = NULL,
                            %s = NULL,
                            %s = NULL,
                            %s = CURRENT_TIMESTAMP
                        WHERE %s = :id
                          AND %s = :pending_status
                          AND %s = :lock_token
                """.formatted(
                        DELIVERY_TABLE_NAME,
                        deliveryColumn(DELIVERY_STATUS_FIELD),
                        deliveryColumn(DELIVERED_AT_FIELD),
                        deliveryColumn(LAST_ERROR_FIELD),
                        deliveryColumn(LOCK_TOKEN_FIELD),
                        deliveryColumn(LOCKED_UNTIL_FIELD),
                        deliveryColumn(MODIFIED_FIELD),
                        deliveryColumn(EMAIL_OUTBOX_ELEMENT_ID_FIELD),
                        deliveryColumn(DELIVERY_STATUS_FIELD),
                        deliveryColumn(LOCK_TOKEN_FIELD)))
                .param("id", id)
                .param("lock_token", lockToken)
                .param("delivery_status", EmailOutboxStatus.SENT.name())
                .param("pending_status", EmailOutboxStatus.PENDING.name())
                .param("delivered_at", deliveredAt)
                .update();
    }

    public int markRetry(@NonNull UUID id,
                         @NonNull UUID lockToken,
                         int attemptCount,
                         @NonNull Timestamp nextAttemptAt,
                         @NonNull String lastError) {
        return jdbcClient.sql("""
                        UPDATE %s
                        SET %s = :attempt_count,
                            %s = :next_attempt_at,
                            %s = :last_error,
                            %s = NULL,
                            %s = NULL,
                            %s = CURRENT_TIMESTAMP
                        WHERE %s = :id
                          AND %s = :delivery_status
                          AND %s = :lock_token
                """.formatted(
                        DELIVERY_TABLE_NAME,
                        deliveryColumn(ATTEMPT_COUNT_FIELD),
                        deliveryColumn(NEXT_ATTEMPT_AT_FIELD),
                        deliveryColumn(LAST_ERROR_FIELD),
                        deliveryColumn(LOCK_TOKEN_FIELD),
                        deliveryColumn(LOCKED_UNTIL_FIELD),
                        deliveryColumn(MODIFIED_FIELD),
                        deliveryColumn(EMAIL_OUTBOX_ELEMENT_ID_FIELD),
                        deliveryColumn(DELIVERY_STATUS_FIELD),
                        deliveryColumn(LOCK_TOKEN_FIELD)))
                .param("id", id)
                .param("lock_token", lockToken)
                .param("delivery_status", EmailOutboxStatus.PENDING.name())
                .param("attempt_count", attemptCount)
                .param("next_attempt_at", nextAttemptAt)
                .param("last_error", lastError)
                .update();
    }

    public int markFailed(@NonNull UUID id,
                          @NonNull UUID lockToken,
                          int attemptCount,
                          @NonNull String lastError) {
        return jdbcClient.sql("""
                        UPDATE %s
                        SET %s = :failed_status,
                            %s = :attempt_count,
                            %s = :last_error,
                            %s = NULL,
                            %s = NULL,
                            %s = CURRENT_TIMESTAMP
                        WHERE %s = :id
                          AND %s = :pending_status
                          AND %s = :lock_token
                """.formatted(
                        DELIVERY_TABLE_NAME,
                        deliveryColumn(DELIVERY_STATUS_FIELD),
                        deliveryColumn(ATTEMPT_COUNT_FIELD),
                        deliveryColumn(LAST_ERROR_FIELD),
                        deliveryColumn(LOCK_TOKEN_FIELD),
                        deliveryColumn(LOCKED_UNTIL_FIELD),
                        deliveryColumn(MODIFIED_FIELD),
                        deliveryColumn(EMAIL_OUTBOX_ELEMENT_ID_FIELD),
                        deliveryColumn(DELIVERY_STATUS_FIELD),
                        deliveryColumn(LOCK_TOKEN_FIELD)))
                .param("id", id)
                .param("lock_token", lockToken)
                .param("failed_status", EmailOutboxStatus.FAILED.name())
                .param("pending_status", EmailOutboxStatus.PENDING.name())
                .param("attempt_count", attemptCount)
                .param("last_error", lastError)
                .update();
    }

    private void insertElement(EmailOutboxElementDbo emailOutboxElement) {
        final LinkedHashMap<String, Object> parameters =
                sqlRecordIntrospector.paramsFromRecord(ELEMENT_TABLE_NAME, emailOutboxElement, Set.of());
        jdbcClient.sql(sqlRecordIntrospector.insertSql(ELEMENT_TABLE_NAME, parameters))
                .params(parameters)
                .update();
    }

    private void insertDelivery(EmailOutboxElementDeliveryDbo delivery) {
        final LinkedHashMap<String, Object> parameters =
                sqlRecordIntrospector.paramsFromRecord(DELIVERY_TABLE_NAME, delivery, Set.of());
        jdbcClient.sql(sqlRecordIntrospector.insertSql(DELIVERY_TABLE_NAME, parameters))
                .params(parameters)
                .update();
    }

    private String returningDeliveryColumns() {
        return sqlRecordIntrospector.csvColumns(sqlRecordIntrospector.qualifiedColumnsFromShape(
                DELIVERY_TABLE_NAME, DELIVERY_ALIAS, EmailOutboxElementDeliveryDbo.class, Set.of()));
    }

    private String selectedElementWithDeliveryColumns() {
        final List<String> selectedColumns = new ArrayList<>();
        selectedColumns.addAll(sqlRecordIntrospector.aliasedColumnsFromShape(
                ELEMENT_TABLE_NAME, ELEMENT_ALIAS, ELEMENT_RESULT_PREFIX, EmailOutboxElementDbo.class, Set.of()));
        selectedColumns.addAll(sqlRecordIntrospector.aliasedColumnsFromShape(
                DELIVERY_TABLE_NAME,
                CLAIMED_DELIVERY_ALIAS,
                DELIVERY_RESULT_PREFIX,
                EmailOutboxElementDeliveryDbo.class,
                Set.of()));
        return sqlRecordIntrospector.csvColumns(selectedColumns);
    }

    private String elementResultColumn(String fieldName) {
        return sqlRecordIntrospector.columnAliasFromField(ELEMENT_TABLE_NAME, ELEMENT_RESULT_PREFIX, fieldName);
    }

    private String deliveryResultColumn(String fieldName) {
        return sqlRecordIntrospector.columnAliasFromField(DELIVERY_TABLE_NAME, DELIVERY_RESULT_PREFIX, fieldName);
    }

    private String deliveryColumn(String fieldName) {
        return sqlRecordIntrospector.columnFromField(DELIVERY_TABLE_NAME, fieldName);
    }

    private String qualifiedElementColumn(String tableAlias, String fieldName) {
        return sqlRecordIntrospector.qualifiedColumnFromField(ELEMENT_TABLE_NAME, tableAlias, fieldName);
    }

    private String qualifiedDeliveryColumn(String tableAlias, String fieldName) {
        return sqlRecordIntrospector.qualifiedColumnFromField(DELIVERY_TABLE_NAME, tableAlias, fieldName);
    }

    private EmailOutboxElementWithDelivery mapElementWithDelivery(ResultSet resultSet, int rowNumber)
            throws SQLException {
        final EmailOutboxElementDbo element = new EmailOutboxElementDbo(
                resultSet.getObject(elementResultColumn(ID_FIELD), UUID.class),
                resultSet.getString(elementResultColumn(RECIPIENT_FIELD)),
                resultSet.getString(elementResultColumn(SUBJECT_FIELD)),
                resultSet.getString(elementResultColumn(BODY_FIELD)));
        final EmailOutboxElementDeliveryDbo delivery = new EmailOutboxElementDeliveryDbo(
                resultSet.getObject(deliveryResultColumn(EMAIL_OUTBOX_ELEMENT_ID_FIELD), UUID.class),
                resultSet.getString(deliveryResultColumn(DELIVERY_STATUS_FIELD)),
                resultSet.getInt(deliveryResultColumn(ATTEMPT_COUNT_FIELD)),
                resultSet.getTimestamp(deliveryResultColumn(NEXT_ATTEMPT_AT_FIELD)),
                resultSet.getTimestamp(deliveryResultColumn(LATEST_DELIVERY_AT_FIELD)),
                resultSet.getTimestamp(deliveryResultColumn(DELIVERED_AT_FIELD)),
                resultSet.getString(deliveryResultColumn(LAST_ERROR_FIELD)),
                resultSet.getObject(deliveryResultColumn(LOCK_TOKEN_FIELD), UUID.class),
                resultSet.getTimestamp(deliveryResultColumn(LOCKED_UNTIL_FIELD)));
        return new EmailOutboxElementWithDelivery(element, delivery);
    }
}
