CREATE TABLE users
(
    id       UUID PRIMARY KEY,
    modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_identity
(
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    modified     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    provider_id  VARCHAR(100) NOT NULL,
    subject      VARCHAR(255) NOT NULL,

    display_name VARCHAR(255),
    email        VARCHAR(320),
    picture_url  TEXT,

    CONSTRAINT user_identity_provider_subject_uq
        UNIQUE (provider_id, subject)
);



CREATE INDEX user_identity_user_id_idx
    ON user_identity (user_id);

CREATE INDEX user_identity_user_provider_id_idx
    ON user_identity (user_id, provider_id);

CREATE TABLE user_login
(
    id          UUID PRIMARY KEY,
    created     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    login_time  TIMESTAMP     NOT NULL,

    provider_id VARCHAR(100)  NOT NULL,
    subject     VARCHAR(255)  NOT NULL,

    client_ip   VARCHAR(64)   NOT NULL,
    user_agent  TEXT NOT NULL DEFAULT ''
);

CREATE INDEX user_login_user_id_idx
    ON user_login (user_id);

CREATE INDEX user_login_login_time_idx
    ON user_login (login_time);

CREATE INDEX user_login_provider_subject_idx
    ON user_login (provider_id, subject);

CREATE TABLE user_profile
(
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,

    modified     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    display_name VARCHAR(20),
    picture_url  VARCHAR(2000)
);


CREATE TABLE user_password_credential
(
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    modified      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email         VARCHAR(320) NOT NULL,
    password_hash TEXT         NOT NULL,

    CONSTRAINT user_password_credential_email_uq
        UNIQUE (email)
);

CREATE TABLE persistent_logins
(
    username  VARCHAR(320) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64)  NOT NULL,
    last_used TIMESTAMP    NOT NULL
);

CREATE INDEX persistent_logins_username_idx
    ON persistent_logins (username);
