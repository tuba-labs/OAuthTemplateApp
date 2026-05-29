package org.tubalabs.app.security.remember;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistentTokenRepository.class)
class UserPersistentTokenRepositoryTest extends AbstractJdbcTestBaseTestClass {

    private static final String USERNAME = "33333333-3333-3333-3333-333333333333";
    private static final String SERIES = "series";
    private static final String TOKEN_VALUE = "token";
    private static final String UPDATED_TOKEN_VALUE = "updated-token";
    private static final Timestamp LAST_USED = Timestamp.from(Instant.parse("2026-05-25T12:00:00Z"));
    private static final Timestamp UPDATED_LAST_USED = Timestamp.from(Instant.parse("2026-05-25T13:00:00Z"));

    @Autowired
    private UserPersistentTokenRepository userPersistentTokenRepository;

    @Test
    void storesUpdatesAndRemovesToken() {
        userPersistentTokenRepository.createNewToken(
                new PersistentRememberMeToken(USERNAME, SERIES, TOKEN_VALUE, LAST_USED));

        assertThat(userPersistentTokenRepository.getTokenForSeries(SERIES))
                .isNotNull()
                .satisfies(token -> assertToken(token, TOKEN_VALUE, LAST_USED));

        userPersistentTokenRepository.updateToken(SERIES, UPDATED_TOKEN_VALUE, UPDATED_LAST_USED);

        assertThat(userPersistentTokenRepository.getTokenForSeries(SERIES))
                .isNotNull()
                .satisfies(token -> assertToken(token, UPDATED_TOKEN_VALUE, UPDATED_LAST_USED));

        userPersistentTokenRepository.removeUserTokens(USERNAME);

        assertThat(userPersistentTokenRepository.getTokenForSeries(SERIES)).isNull();
    }

    @Test
    void returnsNullForUnknownSeries() {
        assertThat(userPersistentTokenRepository.getTokenForSeries(SERIES)).isNull();
    }

    private void assertToken(@NonNull PersistentRememberMeToken token,
                             @NonNull String tokenValue,
                             @NonNull Timestamp lastUsed) {
        assertThat(token.getUsername()).isEqualTo(USERNAME);
        assertThat(token.getSeries()).isEqualTo(SERIES);
        assertThat(token.getTokenValue()).isEqualTo(tokenValue);
        assertThat(token.getDate()).isEqualTo(lastUsed);
    }
}
