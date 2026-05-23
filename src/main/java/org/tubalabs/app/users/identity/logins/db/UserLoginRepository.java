package org.tubalabs.app.users.identity.logins.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;

import java.util.LinkedHashMap;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserLoginRepository {

    private static final String TABLE_NAME = "user_login";

    private final JdbcClient jdbcClient;
    private final SqlRecordIntrospector sqlRecordIntrospector;

    public void insert(@NonNull UserLoginDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        jdbcClient.sql(sqlRecordIntrospector.insertSql(TABLE_NAME, parameters))
                .params(parameters)
                .update();
    }
}
