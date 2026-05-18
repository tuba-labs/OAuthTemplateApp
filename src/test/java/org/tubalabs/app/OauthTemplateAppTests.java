package org.tubalabs.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.tubalabs.app.users.identity.UserIdentityAlreadyExistsException;
import org.tubalabs.app.users.identity.UserIdentityDbo;
import org.tubalabs.app.users.identity.UserIdentityRepository;
import org.tubalabs.app.users.login.UserLoginDbo;
import org.tubalabs.app.users.login.UserLoginRepository;
import org.tubalabs.app.users.profile.UserProfileDbo;
import org.tubalabs.app.users.profile.UserProfileRepository;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class OauthTemplateAppTests {

    private static final UUID USER_ID = UUID.fromString("66f83749-e6de-48c8-88a2-61ba33fe8738");
    private static final UUID OTHER_USER_ID = UUID.fromString("4fb17c31-4b1e-4c90-8043-411ce2496306");
    private static final UUID IDENTITY_ID = UUID.fromString("c9617774-b3aa-4d4c-b469-7d0a510206c2");
    private static final UUID OTHER_IDENTITY_ID = UUID.fromString("f5273344-5e14-4df8-b2a3-4f3cd2fb3403");
    private static final UUID LOGIN_ID = UUID.fromString("61059086-9388-47a3-b935-fb2ce13a851e");
    private static final Timestamp LOGIN_TIME = Timestamp.from(Instant.parse("2026-05-14T11:20:00Z"));
    private static final String PROVIDER_ID = "github";
    private static final String SUBJECT = "github-user-123";
    private static final String DISPLAY_NAME = "Petter";
    private static final String UPDATED_DISPLAY_NAME = "Petter Smart";
    private static final String GIVEN_NAME = "Petter";
    private static final String FAMILY_NAME = "Smart";
    private static final String EMAIL = "petter@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "repository-test";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserLoginRepository userLoginRepository;
    private final JdbcClient jdbcClient;

    @Autowired
    OauthTemplateAppTests(
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            UserProfileRepository userProfileRepository,
            UserLoginRepository userLoginRepository,
            JdbcClient jdbcClient) {

        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userProfileRepository = userProfileRepository;
        this.userLoginRepository = userLoginRepository;
        this.jdbcClient = jdbcClient;
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void repositoriesPersistUserLoginData() {
        final UserDbo user = UserDbo.builder()
                .id(USER_ID)
                .build();
        userRepository.insert(user);

        assertThat(userRepository.findById(USER_ID)).contains(user);

        final UserIdentityDbo identity = UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .displayName(DISPLAY_NAME)
                .givenName(GIVEN_NAME)
                .familyName(FAMILY_NAME)
                .email(EMAIL)
                .pictureUrl(PICTURE_URL)
                .build();

        assertThat(userIdentityRepository.insert(identity)).isEqualTo(identity);
        assertThat(userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT)).contains(identity);

        final UserDbo otherUser = UserDbo.builder()
                .id(OTHER_USER_ID)
                .build();
        userRepository.insert(otherUser);

        final UserIdentityDbo duplicateIdentity = identity.toBuilder()
                .id(OTHER_IDENTITY_ID)
                .userId(OTHER_USER_ID)
                .build();

        assertThatThrownBy(() -> userIdentityRepository.insert(duplicateIdentity))
                .isInstanceOf(UserIdentityAlreadyExistsException.class);

        final UserIdentityDbo requestedIdentityUpdate = identity.toBuilder()
                .displayName(UPDATED_DISPLAY_NAME)
                .build();

        assertThat(userIdentityRepository.update(requestedIdentityUpdate)).isEqualTo(requestedIdentityUpdate);

        final UserProfileDbo profile = UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(DISPLAY_NAME)
                .givenName(GIVEN_NAME)
                .familyName(FAMILY_NAME)
                .email(EMAIL)
                .pictureUrl(PICTURE_URL)
                .build();

        assertThat(userProfileRepository.insert(profile)).isEqualTo(profile);

        userLoginRepository.insert(UserLoginDbo.builder()
                .id(LOGIN_ID)
                .userId(USER_ID)
                .loginTime(LOGIN_TIME)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .clientIp(CLIENT_IP)
                .userAgent(USER_AGENT)
                .build());

        final Integer loginCount = jdbcClient.sql("SELECT COUNT(*) FROM user_login")
                .query(Integer.class)
                .single();

        assertThat(loginCount).isOne();
    }

}
