package org.tubalabs.app.users.identity.externalidentity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.tubalabs.app.etc.TimeConfig;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.events.EventLogService;
import org.tubalabs.app.events.EventType;
import org.tubalabs.app.events.db.EventLogRepository;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;
import org.tubalabs.app.users.identity.logins.UserIdentityAuditLog;
import org.tubalabs.app.users.identity.logins.UserIdentityAuditWriter;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TimeConfig.class,
        SqlColumnNameResolver.class,
        SqlRecordIntrospector.class,
        UserRepository.class,
        UserIdentityRepository.class,
        UserLoginRepository.class,
        EventLogRepository.class,
        EventLogService.class,
        UserIdentityEventFactory.class,
        UserIdentityAuditLog.class,
        UserIdentityAuditWriter.class,
        ExternalIdentityLinkService.class
})
class ExternalIdentityLinkServiceTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID IDENTITY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";
    private static final String OTHER_SUBJECT = "other-google-subject";
    private static final String DISPLAY_NAME = "Person";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private ExternalIdentityLinkService externalIdentityLinkService;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void linksExternalIdentityToUser() {
        insertUser(USER_ID);

        externalIdentityLinkService.link(USER_ID, identity(SUBJECT, DISPLAY_NAME, PICTURE_URL), CLIENT_IP, USER_AGENT);

        final UserIdentityDbo linkedIdentity = userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT)
                .orElseThrow();
        assertThat(linkedIdentity.userId()).isEqualTo(USER_ID);
        assertThat(linkedIdentity.displayName()).isEqualTo(DISPLAY_NAME);
        assertThat(loginCount(USER_ID)).isEqualTo(1);
        assertThat(eventCount(USER_ID, EventType.SIGN_IN_METHOD_LINKED.value())).isEqualTo(1);
    }

    @Test
    void rejectsExternalIdentityAlreadyLinkedToSameUser() {
        insertUser(USER_ID);
        userIdentityRepository.insert(existingIdentity(USER_ID, SUBJECT));

        assertThatExceptionOfType(IdentityLinkException.class)
                .isThrownBy(() -> externalIdentityLinkService.link(
                        USER_ID, identity(SUBJECT, DISPLAY_NAME, PICTURE_URL), CLIENT_IP, USER_AGENT))
                .satisfies(exception -> assertThat(exception.reason())
                        .isEqualTo(IdentityLinkFailure.PROVIDER_ALREADY_LINKED));
        assertThat(loginCount(USER_ID)).isZero();
    }

    @Test
    void rejectsExternalIdentityLinkedToAnotherUser() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
        userIdentityRepository.insert(existingIdentity(OTHER_USER_ID, SUBJECT));

        assertThatExceptionOfType(IdentityLinkException.class)
                .isThrownBy(() -> externalIdentityLinkService.link(
                USER_ID, identity(SUBJECT, DISPLAY_NAME, PICTURE_URL), CLIENT_IP, USER_AGENT))
                .satisfies(exception -> assertThat(exception.reason())
                        .isEqualTo(IdentityLinkFailure.EXTERNAL_IDENTITY_USED));
    }

    @Test
    void rejectsSecondIdentityForSameProvider() {
        insertUser(USER_ID);
        userIdentityRepository.insert(existingIdentity(USER_ID, OTHER_SUBJECT));

        assertThatExceptionOfType(IdentityLinkException.class)
                .isThrownBy(() -> externalIdentityLinkService.link(
                USER_ID, identity(SUBJECT, DISPLAY_NAME, PICTURE_URL), CLIENT_IP, USER_AGENT))
                .satisfies(exception -> assertThat(exception.reason())
                        .isEqualTo(IdentityLinkFailure.PROVIDER_ALREADY_LINKED));
    }

    private void insertUser(UUID userId) {
        userRepository.insert(new UserDbo(userId));
    }

    private UserIdentityDbo existingIdentity(UUID userId, String subject) {
        return UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(userId)
                .providerId(PROVIDER_ID)
                .subject(subject)
                .displayName(DISPLAY_NAME)
                .email(EMAIL)
                .pictureUrl(PICTURE_URL)
                .build();
    }

    private ExternalIdentity identity(String subject, String displayName, String pictureUrl) {
        return ExternalIdentity.builder()
                .providerId(PROVIDER_ID)
                .subject(subject)
                .displayName(displayName)
                .email(EMAIL)
                .avatarUrl(pictureUrl)
                .build();
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
