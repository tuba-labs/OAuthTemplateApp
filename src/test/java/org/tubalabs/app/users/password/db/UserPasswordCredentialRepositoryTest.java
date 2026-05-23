package org.tubalabs.app.users.password.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        SqlColumnNameResolver.class,
        SqlRecordIntrospector.class,
        UserRepository.class,
        UserPasswordCredentialRepository.class
})
class UserPasswordCredentialRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "person@example.com";
    private static final String PASSWORD_HASH = "{noop}ValidPassword1";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPasswordCredentialRepository userPasswordCredentialRepository;

    @Test
    void insertsAndFindsCredential() {
        userRepository.insert(new UserDbo(USER_ID));
        final UserPasswordCredentialDbo credential = UserPasswordCredentialDbo.builder()
                .userId(USER_ID)
                .email(EMAIL)
                .passwordHash(PASSWORD_HASH)
                .build();

        userPasswordCredentialRepository.insert(credential);

        assertThat(userPasswordCredentialRepository.findByEmail(EMAIL)).contains(credential);
    }
}
