package org.tubalabs.app.users.password.db;

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
public class UserPasswordCredentialRepository {

    private static final String TABLE_NAME = "user_password_credential";
    private static final RowMapper<UserPasswordCredentialDbo> USER_PASSWORD_CREDENTIAL_ROW_MAPPER =
            DataClassRowMapper.newInstance(UserPasswordCredentialDbo.class);

    private final JdbcClient jdbcClient;
    private final SqlRecordIntrospector sqlRecordIntrospector;

    public Optional<UserPasswordCredentialDbo> findByEmail(@NonNull String email) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(
                TABLE_NAME, UserPasswordCredentialDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE email = :email")
                .param("email", email)
                .query(USER_PASSWORD_CREDENTIAL_ROW_MAPPER)
                .optional();
    }

    public Optional<UserPasswordCredentialDbo> findByUserId(@NonNull UUID userId) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(
                TABLE_NAME, UserPasswordCredentialDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE user_id = :user_id")
                .param("user_id", userId)
                .query(USER_PASSWORD_CREDENTIAL_ROW_MAPPER)
                .optional();
    }

    public void insert(@NonNull UserPasswordCredentialDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        jdbcClient.sql(sqlRecordIntrospector.insertSql(TABLE_NAME, parameters))
                .params(parameters)
                .update();
    }

    public void updatePasswordHash(@NonNull UUID userId, @NonNull String passwordHash) {
        jdbcClient.sql("""
                        UPDATE user_password_credential
                        SET
                        password_hash = :password_hash,
                        modified = CURRENT_TIMESTAMP
                        WHERE user_id = :user_id
                """)
                .param("user_id", userId)
                .param("password_hash", passwordHash)
                .update();
    }
}
