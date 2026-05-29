package org.tubalabs.app.users.preferences;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;
import org.tubalabs.app.users.settings.UserLanguage;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SqlColumnNameResolver.class, SqlRecordIntrospector.class, UserRepository.class, UserPreferenceRepository.class})
class UserPreferenceRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String LANGUAGE_PREFERENCE_KEY = "language";
    private static final String DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY = "disable_background_animation";
    private static final String ENGLISH_LANGUAGE_TAG = UserLanguage.ENGLISH.tag();
    private static final String NORWEGIAN_LANGUAGE_TAG = UserLanguage.NORWEGIAN.tag();
    private static final String DISABLE_BACKGROUND_ANIMATION = Boolean.TRUE.toString();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Test
    void findsNoPreferencesForUserWithoutPreferences() {
        userRepository.insert(new UserDbo(USER_ID));

        assertThat(userPreferenceRepository.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void upsertsPreference() {
        userRepository.insert(new UserDbo(USER_ID));

        userPreferenceRepository.upsert(USER_ID, LANGUAGE_PREFERENCE_KEY, ENGLISH_LANGUAGE_TAG);
        userPreferenceRepository.upsert(USER_ID, LANGUAGE_PREFERENCE_KEY, NORWEGIAN_LANGUAGE_TAG);

        final UserPreferenceDbo expectedPreference = new UserPreferenceDbo(
                USER_ID,
                LANGUAGE_PREFERENCE_KEY,
                NORWEGIAN_LANGUAGE_TAG);
        assertThat(userPreferenceRepository.findByUserId(USER_ID)).containsExactly(expectedPreference);
    }

    @Test
    void findsAllUserPreferences() {
        userRepository.insert(new UserDbo(USER_ID));

        userPreferenceRepository.upsert(USER_ID, LANGUAGE_PREFERENCE_KEY, NORWEGIAN_LANGUAGE_TAG);
        userPreferenceRepository.upsert(
                USER_ID,
                DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY,
                DISABLE_BACKGROUND_ANIMATION);

        assertThat(userPreferenceRepository.findByUserId(USER_ID))
                .containsExactlyInAnyOrder(
                        new UserPreferenceDbo(
                                USER_ID,
                                LANGUAGE_PREFERENCE_KEY,
                                NORWEGIAN_LANGUAGE_TAG),
                        new UserPreferenceDbo(
                                USER_ID,
                                DISABLE_BACKGROUND_ANIMATION_PREFERENCE_KEY,
                                DISABLE_BACKGROUND_ANIMATION));
    }
}
