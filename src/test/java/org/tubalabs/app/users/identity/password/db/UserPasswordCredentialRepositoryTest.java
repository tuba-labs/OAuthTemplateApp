package org.tubalabs.app.users.identity.password.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialRepository;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private static final UUID OTHER_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String EMAIL = "person@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String PASSWORD_HASH = "{noop}ValidPassword1";
    private static final String UPDATED_PASSWORD_HASH = "{noop}NewValidPassword1";

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
        assertThat(userPasswordCredentialRepository.findByUserId(USER_ID)).contains(credential);
    }

    @Test
    void updatesPasswordHash() {
        userRepository.insert(new UserDbo(USER_ID));
        final UserPasswordCredentialDbo credential = UserPasswordCredentialDbo.builder()
                .userId(USER_ID)
                .email(EMAIL)
                .passwordHash(PASSWORD_HASH)
                .build();
        userPasswordCredentialRepository.insert(credential);

        userPasswordCredentialRepository.updatePasswordHash(USER_ID, UPDATED_PASSWORD_HASH);

        assertThat(userPasswordCredentialRepository.findByUserId(USER_ID))
                .contains(new UserPasswordCredentialDbo(USER_ID, EMAIL, UPDATED_PASSWORD_HASH));
    }

    @Test
    void deletesCredentialByUser() {
        userRepository.insert(new UserDbo(USER_ID));
        final UserPasswordCredentialDbo credential = UserPasswordCredentialDbo.builder()
                .userId(USER_ID)
                .email(EMAIL)
                .passwordHash(PASSWORD_HASH)
                .build();
        userPasswordCredentialRepository.insert(credential);

        final int deletedRows = userPasswordCredentialRepository.deleteByUserId(USER_ID);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(userPasswordCredentialRepository.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void rejectsDuplicateEmail() {
        userRepository.insert(new UserDbo(USER_ID));
        userRepository.insert(new UserDbo(OTHER_USER_ID));
        userPasswordCredentialRepository.insert(credential(USER_ID, EMAIL));

        assertThatThrownBy(() -> userPasswordCredentialRepository.insert(credential(OTHER_USER_ID, EMAIL)))
                .isInstanceOf(UserPasswordCredentialAlreadyExistsException.class);
    }

    @Test
    void rejectsDuplicateUserId() {
        userRepository.insert(new UserDbo(USER_ID));
        userPasswordCredentialRepository.insert(credential(USER_ID, EMAIL));

        assertThatThrownBy(() -> userPasswordCredentialRepository.insert(credential(USER_ID, OTHER_EMAIL)))
                .isInstanceOf(UserPasswordCredentialAlreadyExistsException.class);
    }

    private UserPasswordCredentialDbo credential(UUID userId, String email) {
        return UserPasswordCredentialDbo.builder()
                .userId(userId)
                .email(email)
                .passwordHash(PASSWORD_HASH)
                .build();
    }
}
