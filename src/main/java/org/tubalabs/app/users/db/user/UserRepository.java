package org.tubalabs.app.users.db.user;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private final JdbcClient jdbcClient;

    public Optional<UserDbo> findById(@NonNull UUID id) {
        return jdbcClient.sql("""
                        SELECT id
                        FROM users
                        WHERE id = :id
                        """)
                .param("id", id)
                .query(UserDbo.class)
                .optional();
    }

    public void insert(@NonNull UserDbo dbo) {
        log.debug("Inserting {}", dbo);
        jdbcClient.sql("""
                        INSERT INTO users(
                            id)
                        VALUES(
                            :id
                        )
                        """)
                .paramSource(dbo)
                .update();
    }
}