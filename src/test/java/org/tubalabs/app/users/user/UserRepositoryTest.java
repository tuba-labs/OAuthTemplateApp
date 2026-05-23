package org.tubalabs.app.users.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SqlColumnNameResolver.class, SqlRecordIntrospector.class, UserRepository.class})
class UserRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private UserRepository userRepository;

    @Test
    void insertsAndFindsUser() {
        final UserDbo user = new UserDbo(USER_ID);

        userRepository.insert(user);

        assertThat(userRepository.findById(USER_ID)).contains(user);
    }
}
