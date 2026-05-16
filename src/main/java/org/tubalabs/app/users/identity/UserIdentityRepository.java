package org.tubalabs.app.users.identity;

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

    public UserIdentityDbo insert(@NonNull UserIdentityDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.insertSql(TABLE_NAME, parameters) + sqlRecordIntrospector.returning(parameters.keySet()))
                .params(parameters)
                .query(USER_IDENTITY_ROW_MAPPER)
                .single();
    }

    public UserIdentityDbo update(@NonNull UserIdentityDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        final LinkedHashMap<String, Object> updateParameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of("id"));
        return jdbcClient.sql(sqlRecordIntrospector.updateWithoutWhere(TABLE_NAME, updateParameters)
                        + "WHERE id = :id\n"
                        + sqlRecordIntrospector.returning(parameters.keySet()))
                .params(parameters)
                .query(USER_IDENTITY_ROW_MAPPER)
                .single();
    }
}
