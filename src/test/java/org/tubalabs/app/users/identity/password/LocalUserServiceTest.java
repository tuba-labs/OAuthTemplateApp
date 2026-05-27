package org.tubalabs.app.users.identity.password;

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
import org.tubalabs.app.events.EventLogService;
import org.tubalabs.app.events.EventType;
import org.tubalabs.app.events.db.EventLogRepository;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialAlreadyExistsException;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialRepository;
import org.tubalabs.app.users.identity.password.security.PasswordConfig;
import org.tubalabs.app.users.identity.password.validation.vetoers.UserCreateVetoerService;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.db.UserProfileRepository;
import org.tubalabs.app.users.user.UserDbo;
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
        EventLogRepository.class,
        EventLogService.class,
        UserIdentityEventFactory.class,
        UserPasswordCredentialRepository.class,
        UserProfileRepository.class
})
class LocalUserServiceTest extends AbstractJdbcTestBaseTestClass {

    private static final String EMAIL = "person@example.com";
    private static final String MIXED_CASE_EMAIL = "  Person@Example.COM  ";
    private static final String PASSWORD = "ValidPassword1";
    private static final String NEW_PASSWORD = "NewValidPassword1";
    private static final String WRONG_PASSWORD = "WrongPassword1";
    private static final String DISPLAY_NAME = "Person";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final UUID EXISTING_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private LocalUserService localUserService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

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
        final UserIdentityDbo identity = userIdentityRepository
                .findByProviderAndSubject(LocalUserService.LOCAL_PROVIDER_ID, EMAIL)
                .orElseThrow();
        final UserProfileDbo profile = userProfileRepository.findByUserId(createResult.id()).orElseThrow();

        assertThat(createResult.vetoed()).isFalse();
        assertThat(createResult.id()).isNotNull();
        assertThat(loginResult.identityId()).isEqualTo(identity.id());
        assertThat(loginResult.userId()).isEqualTo(createResult.id());
        assertThat(loginResult.providerId()).isEqualTo(LocalUserService.LOCAL_PROVIDER_ID);
        assertThat(loginResult.subject()).isEqualTo(EMAIL);
        assertThat(credential.userId()).isEqualTo(createResult.id());
        assertThat(passwordEncoder.matches(PASSWORD, credential.passwordHash())).isTrue();
        assertThat(profile.displayName()).isEqualTo(DISPLAY_NAME);
        assertThat(loginCount(createResult.id())).isEqualTo(1);
        assertThat(eventCount(createResult.id(), EventType.USER_LOGIN.value())).isEqualTo(1);
    }

    @Test
    void registersLocalUserWithEmailDerivedDisplayName() {
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));

        final UserProfileDbo profile = userProfileRepository.findByUserId(createResult.id()).orElseThrow();

        assertThat(profile.displayName()).isEqualTo(DISPLAY_NAME);
        assertThat(localUserService.loginName(createResult.id())).contains(EMAIL);
    }

    @Test
    void changesLocalPassword() {
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));

        localUserService.changePassword(createResult.id(), PASSWORD, NEW_PASSWORD);

        final UserPasswordCredentialDbo credential = userPasswordCredentialRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, credential.passwordHash())).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, credential.passwordHash())).isFalse();
        assertThat(localUserService.login(EMAIL, NEW_PASSWORD, CLIENT_IP, USER_AGENT).userId()).isEqualTo(createResult.id());
    }

    @Test
    void linksLocalLoginToExistingUser() {
        userRepository.insert(new UserDbo(EXISTING_USER_ID));

        localUserService.linkLogin(
                EXISTING_USER_ID,
                new LocalUserRegistration(MIXED_CASE_EMAIL, PASSWORD),
                CLIENT_IP,
                USER_AGENT);

        final UserPasswordCredentialDbo credential = userPasswordCredentialRepository.findByEmail(EMAIL).orElseThrow();
        final UserIdentityDbo identity = userIdentityRepository
                .findByProviderAndSubject(LocalUserService.LOCAL_PROVIDER_ID, EMAIL)
                .orElseThrow();

        assertThat(credential.userId()).isEqualTo(EXISTING_USER_ID);
        assertThat(passwordEncoder.matches(PASSWORD, credential.passwordHash())).isTrue();
        assertThat(identity.userId()).isEqualTo(EXISTING_USER_ID);
        assertThat(userProfileRepository.findByUserId(EXISTING_USER_ID)).isEmpty();
        assertThat(loginCount(EXISTING_USER_ID)).isZero();
        assertThat(eventCount(EXISTING_USER_ID, EventType.SIGN_IN_METHOD_LINKED.value())).isEqualTo(1);
        assertThat(eventCount(EXISTING_USER_ID, EventType.USER_LOGIN.value())).isZero();
        assertThat(localUserService.login(EMAIL, PASSWORD, CLIENT_IP, USER_AGENT).userId()).isEqualTo(EXISTING_USER_ID);
        assertThat(loginCount(EXISTING_USER_ID)).isEqualTo(1);
        assertThat(eventCount(EXISTING_USER_ID, EventType.USER_LOGIN.value())).isEqualTo(1);
    }

    @Test
    void rejectsLocalLoginLinkWhenAccountAlreadyHasLocalLogin() {
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));

        assertThatThrownBy(() -> localUserService.linkLogin(
                createResult.id(),
                new LocalUserRegistration("other@example.com", NEW_PASSWORD),
                CLIENT_IP,
                USER_AGENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has email and password");
    }

    @Test
    void rejectsLocalLoginLinkWhenEmailBelongsToAnotherUser() {
        localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));
        userRepository.insert(new UserDbo(EXISTING_USER_ID));

        assertThatThrownBy(() -> localUserService.linkLogin(
                EXISTING_USER_ID,
                new LocalUserRegistration(MIXED_CASE_EMAIL, NEW_PASSWORD),
                CLIENT_IP,
                USER_AGENT))
                .isInstanceOf(LocalUserAlreadyExistsException.class);
    }

    @Test
    void rejectsPasswordChangeWithWrongCurrentPassword() {
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD));

        assertThatThrownBy(() -> localUserService.changePassword(createResult.id(), WRONG_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(BadCredentialsException.class);
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

    @Test
    void translatesCredentialDuplicateToLocalUserAlreadyExists() {
        userRepository.insert(new UserDbo(EXISTING_USER_ID));
        userPasswordCredentialRepository.insert(UserPasswordCredentialDbo.builder()
                .userId(EXISTING_USER_ID)
                .email(EMAIL)
                .passwordHash(PASSWORD)
                .build());

        assertThatThrownBy(() -> localUserService.register(new LocalUserRegistration(EMAIL, PASSWORD)))
                .isInstanceOf(LocalUserAlreadyExistsException.class)
                .hasCauseInstanceOf(UserPasswordCredentialAlreadyExistsException.class);
    }

    private int loginCount(UUID userId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM user_login WHERE user_id = :user_id")
                .param("user_id", userId)
                .query(Integer.class)
                .single();
    }

    private int eventCount(UUID userId, String eventType) {
        return jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM event_log
                        WHERE actor_user_id = :user_id
                          AND event_type = :event_type
                """)
                .param("user_id", userId)
                .param("event_type", eventType)
                .query(Integer.class)
                .single();
    }
}
