CREATE TABLE user_settings
(
    user_id                     UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    modified                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remember_login_prompt_after TIMESTAMP
);
