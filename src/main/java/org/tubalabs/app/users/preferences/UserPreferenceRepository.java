package org.tubalabs.app.users.preferences;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserPreferenceRepository {

    private static final RowMapper<UserPreferenceDbo> USER_PREFERENCE_ROW_MAPPER =
            DataClassRowMapper.newInstance(UserPreferenceDbo.class);

    private final JdbcClient jdbcClient;

    public List<UserPreferenceDbo> findByUserId(@NonNull UUID userId) {
        return jdbcClient.sql("""
                        SELECT user_id, preference_key, preference_value
                        FROM user_preferences
                        WHERE user_id = :user_id
                        ORDER BY preference_key
                """)
                .param("user_id", userId)
                .query(USER_PREFERENCE_ROW_MAPPER)
                .list();
    }

    public void upsert(@NonNull UUID userId, @NonNull String preferenceKey, @NonNull String preferenceValue) {
        jdbcClient.sql("""
                        INSERT INTO user_preferences (user_id, preference_key, preference_value)
                        VALUES (:user_id, :preference_key, :preference_value)
                        ON CONFLICT (user_id, preference_key) DO UPDATE
                        SET
                        preference_value = EXCLUDED.preference_value,
                        modified = CURRENT_TIMESTAMP
                """)
                .param("user_id", userId)
                .param("preference_key", preferenceKey)
                .param("preference_value", preferenceValue)
                .update();
    }
}
