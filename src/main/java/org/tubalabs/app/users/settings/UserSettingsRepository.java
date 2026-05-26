package org.tubalabs.app.users.settings;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserSettingsRepository {

    private static final String TABLE_NAME = "user_settings";
    private static final RowMapper<UserSettingsDbo> USER_SETTINGS_ROW_MAPPER = DataClassRowMapper.newInstance(UserSettingsDbo.class);

    private final JdbcClient jdbcClient;
    private final SqlRecordIntrospector sqlRecordIntrospector;

    public Optional<UserSettingsDbo> findByUserId(@NonNull UUID userId) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(TABLE_NAME, UserSettingsDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE user_id = :user_id")
                .param("user_id", userId)
                .query(USER_SETTINGS_ROW_MAPPER)
                .optional();
    }

    public void upsertRememberLoginPromptAfter(@NonNull UUID userId, @NonNull Timestamp rememberLoginPromptAfter) {
        jdbcClient.sql("""
                        INSERT INTO user_settings (user_id, remember_login_prompt_after)
                        VALUES (:user_id, :remember_login_prompt_after)
                        ON CONFLICT (user_id) DO UPDATE
                        SET
                        remember_login_prompt_after = EXCLUDED.remember_login_prompt_after,
                        modified = CURRENT_TIMESTAMP
                """)
                .param("user_id", userId)
                .param("remember_login_prompt_after", rememberLoginPromptAfter)
                .update();
    }

    public void clearRememberLoginPromptAfter(@NonNull UUID userId) {
        jdbcClient.sql("""
                        INSERT INTO user_settings (user_id, remember_login_prompt_after)
                        VALUES (:user_id, NULL)
                        ON CONFLICT (user_id) DO UPDATE
                        SET
                        remember_login_prompt_after = NULL,
                        modified = CURRENT_TIMESTAMP
                """)
                .param("user_id", userId)
                .update();
    }

    public void upsertLanguageTag(@NonNull UUID userId, @NonNull String languageTag) {
        jdbcClient.sql("""
                        INSERT INTO user_settings (user_id, language_tag)
                        VALUES (:user_id, :language_tag)
                        ON CONFLICT (user_id) DO UPDATE
                        SET
                        language_tag = EXCLUDED.language_tag,
                        modified = CURRENT_TIMESTAMP
                """)
                .param("user_id", userId)
                .param("language_tag", languageTag)
                .update();
    }
}
