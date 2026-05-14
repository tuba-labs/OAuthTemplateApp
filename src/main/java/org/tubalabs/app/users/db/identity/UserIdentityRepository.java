package org.tubalabs.app.users.db.identity;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserIdentityRepository {

    private final JdbcClient jdbcClient;

    public Optional<UserIdentityDbo> findByProviderAndSubject(
            @NonNull String providerId,
            @NonNull String subject) {

        return jdbcClient.sql("""
                        SELECT id,
                               user_id,
                               provider_id,
                               subject,
                               display_name,
                               given_name,
                               family_name,
                               email,
                               picture_url
                        FROM user_identity
                        WHERE provider_id = :providerId
                          AND subject = :subject
                        """)
                .param("providerId", providerId)
                .param("subject", subject)
                .query(UserIdentityDbo.class)
                .optional();
    }
    public UserIdentityDbo insert(@NonNull UserIdentityDbo dbo) {

        log.debug("Inserting {}", dbo);

        return jdbcClient.sql("""
                    INSERT INTO user_identity(
                        id,
                        user_id,
                        provider_id,
                        subject,
                        display_name,
                        given_name,
                        family_name,
                        email,
                        picture_url
                    )
                    VALUES(
                        :id,
                        :userId,
                        :providerId,
                        :subject,
                        :displayName,
                        :givenName,
                        :familyName,
                        :email,
                        :pictureUrl
                    )
                    RETURNING id,
                              user_id,
                              provider_id,
                              subject,
                              display_name,
                              given_name,
                              family_name,
                              email,
                              picture_url
                    """)
                .paramSource(dbo)
                .query(UserIdentityDbo.class)
                .single();
    }
    public UserIdentityDbo update(@NonNull UserIdentityDbo dbo) {

        log.debug("Updating {}", dbo);

        return jdbcClient.sql("""
                    UPDATE user_identity
                    SET modified = CURRENT_TIMESTAMP,
                        display_name = :displayName,
                        given_name = :givenName,
                        family_name = :familyName,
                        email = :email,
                        picture_url = :pictureUrl
                    WHERE id = :id
                    RETURNING id,
                              user_id,
                              provider_id,
                              subject,
                              display_name,
                              given_name,
                              family_name,
                              email,
                              picture_url
                    """)
                .paramSource(dbo)
                .query(UserIdentityDbo.class)
                .single();
    }
}