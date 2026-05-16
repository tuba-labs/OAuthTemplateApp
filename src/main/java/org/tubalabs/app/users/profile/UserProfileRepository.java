package org.tubalabs.app.users.profile;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;

import java.util.LinkedHashMap;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserProfileRepository {

    private static final String TABLE_NAME = "user_profile";
    private static final RowMapper<UserProfileDbo> USER_PROFILE_ROW_MAPPER = DataClassRowMapper.newInstance(UserProfileDbo.class);

    private final JdbcClient jdbcClient;
    private final SqlRecordIntrospector sqlRecordIntrospector;

    public UserProfileDbo insert(@NonNull UserProfileDbo dbo) {
        final LinkedHashMap<String, Object> parameters = sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, dbo, Set.of());
        return jdbcClient.sql(sqlRecordIntrospector.insertSql(TABLE_NAME, parameters) + sqlRecordIntrospector.returning(parameters.keySet()))
                .params(parameters)
                .query(USER_PROFILE_ROW_MAPPER)
                .single();
    }

}
