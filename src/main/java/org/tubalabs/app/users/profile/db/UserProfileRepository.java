package org.tubalabs.app.users.profile.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserProfileRepository {

    private static final String TABLE_NAME = "user_profile";
    private static final RowMapper<UserProfileDbo> USER_PROFILE_ROW_MAPPER = DataClassRowMapper.newInstance(UserProfileDbo.class);

    private final JdbcClient jdbcClient;
    private final SqlRecordIntrospector sqlRecordIntrospector;

    public Optional<UserProfileDbo> findByUserId(@NonNull UUID userId) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(TABLE_NAME, UserProfileDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE user_id = :user_id")
                .param("user_id", userId)
                .query(USER_PROFILE_ROW_MAPPER)
                .optional();
    }

    public UserProfileDbo insert(@NonNull UserProfileDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.insertSql(TABLE_NAME, parameters) + sqlRecordIntrospector.returning(parameters.keySet()))
                .params(parameters)
                .query(USER_PROFILE_ROW_MAPPER)
                .single();
    }

    public UserProfileDbo update(@NonNull UserProfileDbo dbo) {
        return jdbcClient.sql("""
                        UPDATE user_profile
                        SET
                        display_name = :display_name,
                        picture_url = :picture_url,
                        modified = CURRENT_TIMESTAMP
                        WHERE user_id = :user_id
                        RETURNING
                        user_id,
                        display_name,
                        email,
                        picture_url
                """)
                .param("user_id", dbo.userId())
                .param("display_name", dbo.displayName())
                .param("picture_url", dbo.pictureUrl())
                .query(USER_PROFILE_ROW_MAPPER)
                .single();
    }

}
