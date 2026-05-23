package org.tubalabs.app.users.identity.logins.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SqlColumnNameResolver.class, SqlRecordIntrospector.class, UserRepository.class, UserLoginRepository.class})
class UserLoginRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LOGIN_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String PROVIDER_ID = "local";
    private static final String SUBJECT = "person@example.com";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final Instant LOGIN_TIME = Instant.parse("2026-05-23T12:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLoginRepository userLoginRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void insertsLogin() {
        userRepository.insert(new UserDbo(USER_ID));
        final UserLoginDbo login = UserLoginDbo.builder()
                .id(LOGIN_ID)
                .userId(USER_ID)
                .loginTime(Timestamp.from(LOGIN_TIME))
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .clientIp(CLIENT_IP)
                .userAgent(USER_AGENT)
                .build();

        userLoginRepository.insert(login);

        assertThat(loginCount(LOGIN_ID)).isEqualTo(1);
    }

    private int loginCount(UUID loginId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM user_login WHERE id = :id")
                .param("id", loginId)
                .query(Integer.class)
                .single();
    }
}
