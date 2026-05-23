package org.tubalabs.app.users.password;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tubalabs.app.etc.TimeConfig;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.login.db.UserLoginRepository;
import org.tubalabs.app.users.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.password.db.UserPasswordCredentialRepository;
import org.tubalabs.app.users.password.security.PasswordConfig;
import org.tubalabs.app.users.password.validation.vetoers.UserCreateVetoerService;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.db.UserProfileRepository;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TimeConfig.class,
        SqlColumnNameResolver.class,
        SqlRecordIntrospector.class,
        PasswordConfig.class,
        UserCreateVetoerService.class,
        LocalEmailNormalizer.class,
        LocalUserService.class,
        UserProfileService.class,
        UserRepository.class,
        UserIdentityRepository.class,
        UserLoginRepository.class,
        UserPasswordCredentialRepository.class,
        UserProfileRepository.class
})
class LocalUserServiceTest extends AbstractJdbcTestBaseTestClass {

    private static final String EMAIL = "person@example.com";
    private static final String MIXED_CASE_EMAIL = "  Person@Example.COM  ";
    private static final String PASSWORD = "ValidPassword1";
    private static final String WRONG_PASSWORD = "WrongPassword1";
    private static final String DISPLAY_NAME = "Person";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";

    @Autowired
    private LocalUserService localUserService;

    @Autowired
    private UserPasswordCredentialRepository userPasswordCredentialRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void registersAndLogsInLocalUser() {
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(
                MIXED_CASE_EMAIL, PASSWORD));

        final LoginResult loginResult = localUserService.login(MIXED_CASE_EMAIL, PASSWORD, CLIENT_IP, USER_AGENT);
        final UserPasswordCredentialDbo credential = userPasswordCredentialRepository.findByEmail(EMAIL).orElseThrow();
        final UserProfileDbo profile = userProfileRepository.findByUserId(createResult.id()).orElseThrow();

        assertThat(createResult.vetoed()).isFalse();
        assertThat(createResult.id()).isNotNull();
        assertThat(loginResult.userId()).isEqualTo(createResult.id());
        assertThat(loginResult.providerId()).isEqualTo(LocalUserService.LOCAL_PROVIDER_ID);
        assertThat(loginResult.subject()).isEqualTo(EMAIL);
        assertThat(credential.userId()).isEqualTo(createResult.id());
        assertThat(passwordEncoder.matches(PASSWORD, credential.passwordHash())).isTrue();
        assertThat(profile.displayName()).isEqualTo(DISPLAY_NAME);
        assertThat(profile.email()).isEqualTo(EMAIL);
        assertThat(loginCount(createResult.id())).isEqualTo(1);
    }

    @Test
    void registersLocalUserWithEmailDerivedDisplayName() {
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));

        final UserProfileDbo profile = userProfileRepository.findByUserId(createResult.id()).orElseThrow();

        assertThat(profile.displayName()).isEqualTo(DISPLAY_NAME);
        assertThat(profile.email()).isEqualTo(EMAIL);
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));

        assertThatThrownBy(() -> localUserService.login(EMAIL, WRONG_PASSWORD, CLIENT_IP, USER_AGENT))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsDuplicateLocalEmail() {
        localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));

        assertThatThrownBy(() -> localUserService.register(new LocalUserRegistration(
                MIXED_CASE_EMAIL, PASSWORD)))
                .isInstanceOf(LocalUserAlreadyExistsException.class);
    }

    private int loginCount(UUID userId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM user_login WHERE user_id = :user_id")
                .param("user_id", userId)
                .query(Integer.class)
                .single();
    }
}
