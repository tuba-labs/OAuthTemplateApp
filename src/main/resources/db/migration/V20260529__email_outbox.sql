CREATE TABLE email_outbox_element
(
    id        UUID PRIMARY KEY,
    modified  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    recipient VARCHAR(320) NOT NULL,
    subject   TEXT         NOT NULL,
    body      TEXT         NOT NULL,

    CONSTRAINT email_outbox_element_recipient_not_blank_ck
        CHECK (LENGTH(TRIM(recipient)) > 0),
    CONSTRAINT email_outbox_element_subject_not_blank_ck
        CHECK (LENGTH(TRIM(subject)) > 0)
);

CREATE TABLE email_outbox_element_delivery
(
    email_outbox_element_id UUID PRIMARY KEY
        REFERENCES email_outbox_element (id)
            ON DELETE CASCADE,
    modified                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created                 TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    delivery_status         VARCHAR(20) NOT NULL,
    attempt_count           INTEGER     NOT NULL DEFAULT 0,
    next_attempt_at         TIMESTAMP   NOT NULL,
    latest_delivery_at      TIMESTAMP   NOT NULL,
    delivered_at            TIMESTAMP,
    last_error              TEXT,
    lock_token              UUID,
    locked_until            TIMESTAMP,

    CONSTRAINT email_outbox_delivery_status_ck
        CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT email_outbox_delivery_attempt_count_ck
        CHECK (attempt_count >= 0),
    CONSTRAINT email_outbox_delivery_latest_after_next_ck
        CHECK (latest_delivery_at >= next_attempt_at),
    CONSTRAINT email_outbox_delivery_sent_delivered_at_ck
        CHECK (delivery_status <> 'SENT' OR delivered_at IS NOT NULL),
    CONSTRAINT email_outbox_delivery_lock_fields_ck
        CHECK (
            (lock_token IS NULL AND locked_until IS NULL)
            OR
            (lock_token IS NOT NULL AND locked_until IS NOT NULL)
        )
);

CREATE INDEX email_outbox_element_delivery_claim_due_idx
    ON email_outbox_element_delivery (delivery_status, next_attempt_at, latest_delivery_at, locked_until);

CREATE INDEX email_outbox_element_delivery_lock_token_idx
    ON email_outbox_element_delivery (lock_token)
    WHERE lock_token IS NOT NULL;
