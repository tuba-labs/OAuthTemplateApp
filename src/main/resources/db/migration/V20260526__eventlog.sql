
CREATE TABLE event_log
(
    id            UUID PRIMARY KEY,
    created       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    event_type    VARCHAR(100) NOT NULL,
    actor_user_id UUID,

    subject_type  VARCHAR(100),
    subject_id    VARCHAR(255),

    client_ip     VARCHAR(64),
    user_agent    TEXT         NOT NULL DEFAULT '',
    details       JSONB        NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX event_log_created_idx
    ON event_log (created);

CREATE INDEX event_log_event_type_idx
    ON event_log (event_type);

CREATE INDEX event_log_actor_user_id_idx
    ON event_log (actor_user_id);

CREATE INDEX event_log_subject_idx
    ON event_log (subject_type, subject_id);
