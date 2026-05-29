package org.tubalabs.app.users.profile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.profile.db.UserProfileRepository;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tubalabs.app.users.profile.UserProfileConstraints.DISPLAY_NAME_MAX_LENGTH_MESSAGE;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SqlColumnNameResolver.class, SqlRecordIntrospector.class, UserRepository.class, UserProfileRepository.class, UserProfileService.class})
class UserProfileServiceTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person";
    private static final String UPDATED_DISPLAY_NAME = "Updated Person";
    private static final String LONG_DISPLAY_NAME = " abcdefghijklmnopqrstuvwxyz ";
    private static final String TRUNCATED_DISPLAY_NAME = "abcdefghijklmnopqrst";
    private static final String PICTURE_URL = "https://example.com/avatar.png";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Test
    void updatesEditableProfileFieldsAndTrimsFormValues() {
        insertProfile();

        final UserProfileDbo updatedProfile = userProfileService.updateProfile(
                USER_ID,
                new UserProfileUpdate(" " + UPDATED_DISPLAY_NAME + " ", " "));

        assertThat(updatedProfile.displayName()).isEqualTo(UPDATED_DISPLAY_NAME);
        assertThat(updatedProfile.pictureUrl()).isNull();
        assertThat(updatedProfile.profileComplete()).isTrue();
        assertThat(userProfileRepository.findByUserId(USER_ID)).contains(updatedProfile);
    }

    @Test
    void rejectsDisplayNamesLongerThanProfileLimit() {
        insertProfile();

        assertThatThrownBy(() -> userProfileService.updateProfile(
                USER_ID,
                new UserProfileUpdate(LONG_DISPLAY_NAME, PICTURE_URL)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(DISPLAY_NAME_MAX_LENGTH_MESSAGE);
    }

    @Test
    void capsInitialDisplayNameToProfileLimit() {
        userRepository.insert(new UserDbo(USER_ID));

        final UserProfileDbo profile = userProfileService.createInitialProfile(
                USER_ID,
                LONG_DISPLAY_NAME,
                PICTURE_URL);

        assertThat(profile.displayName()).isEqualTo(TRUNCATED_DISPLAY_NAME);
        assertThat(profile.profileComplete()).isFalse();
        assertThat(userProfileRepository.findByUserId(USER_ID)).contains(profile);
    }

    private void insertProfile() {
        userRepository.insert(new UserDbo(USER_ID));
        userProfileRepository.insert(UserProfileDbo.builder()
                .userId(USER_ID)
                .displayName(DISPLAY_NAME)
                .pictureUrl(PICTURE_URL)
                .build());
    }
}
