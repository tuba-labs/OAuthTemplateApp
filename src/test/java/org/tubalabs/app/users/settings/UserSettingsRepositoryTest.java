package org.tubalabs.app.users.settings;

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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SqlColumnNameResolver.class, SqlRecordIntrospector.class, UserRepository.class, UserSettingsRepository.class})
class UserSettingsRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant REMEMBER_LOGIN_PROMPT_AFTER = Instant.parse("2026-06-22T12:00:00Z");
    private static final Instant UPDATED_REMEMBER_LOGIN_PROMPT_AFTER = Instant.parse("2026-07-22T12:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Test
    void findsNoSettingsForUserWithoutSettings() {
        userRepository.insert(new UserDbo(USER_ID));

        assertThat(userSettingsRepository.findByUserId(USER_ID)).isEmpty();
    }

    @Test
    void upsertsRememberLoginPromptAfter() {
        userRepository.insert(new UserDbo(USER_ID));
        final Timestamp rememberLoginPromptAfter = Timestamp.from(REMEMBER_LOGIN_PROMPT_AFTER);
        final Timestamp updatedRememberLoginPromptAfter = Timestamp.from(UPDATED_REMEMBER_LOGIN_PROMPT_AFTER);

        userSettingsRepository.upsertRememberLoginPromptAfter(USER_ID, rememberLoginPromptAfter);
        userSettingsRepository.upsertRememberLoginPromptAfter(USER_ID, updatedRememberLoginPromptAfter);

        final UserSettingsDbo expectedSettings = new UserSettingsDbo(USER_ID, updatedRememberLoginPromptAfter);
        assertThat(userSettingsRepository.findByUserId(USER_ID)).contains(expectedSettings);
    }

    @Test
    void clearsRememberLoginPromptAfter() {
        userRepository.insert(new UserDbo(USER_ID));
        userSettingsRepository.upsertRememberLoginPromptAfter(USER_ID, Timestamp.from(REMEMBER_LOGIN_PROMPT_AFTER));

        userSettingsRepository.clearRememberLoginPromptAfter(USER_ID);

        final UserSettingsDbo expectedSettings = new UserSettingsDbo(USER_ID, null);
        assertThat(userSettingsRepository.findByUserId(USER_ID)).contains(expectedSettings);
    }
}
