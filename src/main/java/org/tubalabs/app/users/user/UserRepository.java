package org.tubalabs.app.users.user;

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
public class UserRepository {

    private static final String TABLE_NAME = "users";
    private static final RowMapper<UserDbo> USER_ROW_MAPPER = DataClassRowMapper.newInstance(UserDbo.class);

    private final SqlRecordIntrospector sqlRecordIntrospector;
    private final JdbcClient jdbcClient;

    public Optional<UserDbo> findById(@NonNull UUID id) {
        final List<String> columns = sqlRecordIntrospector.columnsFromShape(TABLE_NAME, UserDbo.class, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.select(TABLE_NAME, columns) + "WHERE id = :id")
                .param("id", id)
                .query(USER_ROW_MAPPER)
                .optional();
    }

    public void insert(@NonNull UserDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        jdbcClient.sql(sqlRecordIntrospector.insertSql(TABLE_NAME, parameters))
                .params(parameters)
                .update();
    }
}
