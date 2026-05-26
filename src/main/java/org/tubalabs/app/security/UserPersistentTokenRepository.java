package org.tubalabs.app.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Date;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserPersistentTokenRepository implements PersistentTokenRepository {

    private static final RowMapper<PersistentRememberMeToken> TOKEN_ROW_MAPPER =
            (resultSet, rowNumber) -> new PersistentRememberMeToken(
                    resultSet.getString("username"),
                    resultSet.getString("series"),
                    resultSet.getString("token"),
                    resultSet.getTimestamp("last_used"));

    private final @NonNull JdbcClient jdbcClient;

    @Override
    public void createNewToken(@NonNull PersistentRememberMeToken token) {
        jdbcClient.sql("""
                        INSERT INTO user_persistent_logins (username, series, token, last_used)
                        VALUES (:username, :series, :token, :last_used)
                """)
                .param("username", token.getUsername())
                .param("series", token.getSeries())
                .param("token", token.getTokenValue())
                .param("last_used", timestamp(token.getDate()))
                .update();
    }

    @Override
    public void updateToken(@NonNull String series, @NonNull String tokenValue, @NonNull Date lastUsed) {
        jdbcClient.sql("""
                        UPDATE user_persistent_logins
                        SET token = :token,
                            last_used = :last_used
                        WHERE series = :series
                """)
                .param("series", series)
                .param("token", tokenValue)
                .param("last_used", timestamp(lastUsed))
                .update();
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(@NonNull String seriesId) {
        try {
            return jdbcClient.sql("""
                            SELECT username, series, token, last_used
                            FROM user_persistent_logins
                            WHERE series = :series
                    """)
                    .param("series", seriesId)
                    .query(TOKEN_ROW_MAPPER)
                    .optional()
                    .orElse(null);
        } catch (DataAccessException exception) {
            log.error("Failed to load remember-me token for series {}", seriesId, exception);
            return null;
        }
    }

    @Override
    public void removeUserTokens(@NonNull String username) {
        jdbcClient.sql("""
                        DELETE FROM user_persistent_logins
                        WHERE username = :username
                """)
                .param("username", username)
                .update();
    }

    private Timestamp timestamp(@NonNull Date date) {
        return new Timestamp(date.getTime());
    }
}
