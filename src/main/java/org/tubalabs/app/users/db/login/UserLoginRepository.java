package org.tubalabs.app.users.db.login;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserLoginRepository {

    private final JdbcClient jdbcClient;

    public void insert(@NonNull UserLoginDbo dbo) {
        log.debug("Inserting {}", dbo);
        jdbcClient.sql("""
                        INSERT INTO user_login(
                            id,
                            user_id,
                            login_time,
                            provider_id,
                            subject,
                            client_ip,
                            user_agent
                        )
                        VALUES(
                            :id,
                            :userId,
                            :loginTime,
                            :providerId,
                            :subject,
                            :clientIp,
                            :userAgent
                        )
                        """)
                .paramSource(dbo)
                .update();
    }
}