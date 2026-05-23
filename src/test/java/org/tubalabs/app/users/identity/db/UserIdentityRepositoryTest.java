package org.tubalabs.app.users.identity.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.identity.UserIdentityAlreadyExistsException;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SqlColumnNameResolver.class, SqlRecordIntrospector.class, UserRepository.class, UserIdentityRepository.class})
class UserIdentityRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID IDENTITY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID OTHER_IDENTITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String PROVIDER_ID = "local";
    private static final String SUBJECT = "person@example.com";
    private static final String DISPLAY_NAME = "Person";
    private static final String UPDATED_DISPLAY_NAME = "Updated Person";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Test
    void insertsFindsAndUpdatesIdentity() {
        insertUser(USER_ID);
        final UserIdentityDbo identity = newIdentity(IDENTITY_ID, USER_ID, DISPLAY_NAME);

        final UserIdentityDbo insertedIdentity = userIdentityRepository.insert(identity);
        final UserIdentityDbo updatedIdentity = insertedIdentity.toBuilder()
                .displayName(UPDATED_DISPLAY_NAME)
                .build();

        assertThat(userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT)).contains(identity);
        assertThat(userIdentityRepository.update(updatedIdentity)).isEqualTo(updatedIdentity);
        assertThat(userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT)).contains(updatedIdentity);
    }

    @Test
    void rejectsDuplicateProviderAndSubject() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
        userIdentityRepository.insert(newIdentity(IDENTITY_ID, USER_ID, DISPLAY_NAME));

        final UserIdentityDbo duplicateIdentity = newIdentity(OTHER_IDENTITY_ID, OTHER_USER_ID, DISPLAY_NAME);

        assertThatThrownBy(() -> userIdentityRepository.insert(duplicateIdentity))
                .isInstanceOf(UserIdentityAlreadyExistsException.class);
    }

    private void insertUser(UUID userId) {
        userRepository.insert(new UserDbo(userId));
    }

    private UserIdentityDbo newIdentity(UUID identityId, UUID userId, String displayName) {
        return UserIdentityDbo.builder()
                .id(identityId)
                .userId(userId)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .displayName(displayName)
                .email(EMAIL)
                .pictureUrl(PICTURE_URL)
                .build();
    }
}
