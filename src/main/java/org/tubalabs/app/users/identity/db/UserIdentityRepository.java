package org.tubalabs.app.users.identity.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.users.identity.UserIdentityAlreadyExistsException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserIdentityRepository {

    private static final String TABLE_NAME = "user_identity";
    private static final RowMapper<UserIdentityDbo> USER_IDENTITY_ROW_MAPPER =
            DataClassRowMapper.newInstance(UserIdentityDbo.class);

    private final JdbcClient jdbcClient;
    private final SqlRecordIntrospector sqlRecordIntrospector;

    public Optional<UserIdentityDbo> findByProviderAndSubject(@NonNull String providerId,
                                                              @NonNull String subject) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(TABLE_NAME, UserIdentityDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE provider_id = :provider_id AND subject = :subject")
                .param("provider_id", providerId)
                .param("subject", subject)
                .query(USER_IDENTITY_ROW_MAPPER)
                .optional();
    }

    public Optional<UserIdentityDbo> findById(@NonNull UUID id) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(TABLE_NAME, UserIdentityDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE id = :id")
                .param("id", id)
                .query(USER_IDENTITY_ROW_MAPPER)
                .optional();
    }

    public List<UserIdentityDbo> findByUserId(@NonNull UUID userId) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(TABLE_NAME, UserIdentityDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE user_id = :user_id ORDER BY provider_id")
                .param("user_id", userId)
                .query(USER_IDENTITY_ROW_MAPPER)
                .list();
    }

    public Optional<UserIdentityDbo> findByUserIdAndProviderId(@NonNull UUID userId, @NonNull String providerId) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(TABLE_NAME, UserIdentityDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE user_id = :user_id AND provider_id = :provider_id")
                .param("user_id", userId)
                .param("provider_id", providerId)
                .query(USER_IDENTITY_ROW_MAPPER)
                .optional();
    }

    public UserIdentityDbo insert(@NonNull UserIdentityDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.insertSql(TABLE_NAME, parameters)
                        + "ON CONFLICT (provider_id, subject) DO NOTHING\n"
                        + sqlRecordIntrospector.returning(parameters.keySet()))
                .params(parameters)
                .query(USER_IDENTITY_ROW_MAPPER)
                .optional()
                .orElseThrow(() -> new UserIdentityAlreadyExistsException(dbo.providerId(), dbo.subject()));
    }

    public UserIdentityDbo update(@NonNull UserIdentityDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        final LinkedHashMap<String, Object> updateParameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of("id"));
        return jdbcClient.sql(sqlRecordIntrospector.updateWithoutWhere(TABLE_NAME, updateParameters)
                        + SqlRecordIntrospector.SQL_COLUMN_SEPARATOR
                        + "modified = CURRENT_TIMESTAMP\n"
                        + "WHERE id = :id\n"
                        + sqlRecordIntrospector.returning(parameters.keySet()))
                .params(parameters)
                .query(USER_IDENTITY_ROW_MAPPER)
                .single();
    }

    public int deleteByUserIdAndProviderId(@NonNull UUID userId, @NonNull String providerId) {
        return jdbcClient.sql("""
                        DELETE FROM user_identity
                        WHERE user_id = :user_id AND provider_id = :provider_id
                """)
                .param("user_id", userId)
                .param("provider_id", providerId)
                .update();
    }
}
