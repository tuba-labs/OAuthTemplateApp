package org.tubalabs.app.users.profile.db;

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
@Import({SqlColumnNameResolver.class, SqlRecordIntrospector.class, UserRepository.class, UserProfileRepository.class})
class UserProfileRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person";
    private static final String UPDATED_DISPLAY_NAME = "Updated Person";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String UPDATED_PICTURE_URL = "https://example.com/updated-avatar.png";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void insertsAndFindsProfile() {
        userRepository.insert(new UserDbo(USER_ID));
        final UserProfileDbo profile = UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(DISPLAY_NAME)
                .pictureUrl(PICTURE_URL)
                .build();

        final UserProfileDbo insertedProfile = userProfileRepository.insert(profile);

        assertThat(insertedProfile).isEqualTo(profile);
        assertThat(userProfileRepository.findByUserId(USER_ID)).contains(profile);
    }

    @Test
    void updatesProfile() {
        userRepository.insert(new UserDbo(USER_ID));
        userProfileRepository.insert(UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(DISPLAY_NAME)
                .pictureUrl(PICTURE_URL)
                .build());
        final UserProfileDbo updatedProfile = UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(UPDATED_DISPLAY_NAME)
                .pictureUrl(UPDATED_PICTURE_URL)
                .profileComplete(true)
                .build();
        final UserProfileDbo expectedProfile = UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(UPDATED_DISPLAY_NAME)
                .pictureUrl(UPDATED_PICTURE_URL)
                .profileComplete(true)
                .build();

        final UserProfileDbo savedProfile = userProfileRepository.update(updatedProfile);

        assertThat(savedProfile).isEqualTo(expectedProfile);
        assertThat(userProfileRepository.findByUserId(USER_ID)).contains(expectedProfile);
    }
}
