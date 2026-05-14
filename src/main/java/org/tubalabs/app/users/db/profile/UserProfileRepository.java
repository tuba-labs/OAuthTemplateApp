package org.tubalabs.app.users.db.profile;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserProfileRepository {

    private final JdbcClient jdbcClient;
    public UserProfileDbo insert(@NonNull UserProfileDbo dbo) {

        log.debug("Inserting {}", dbo);

        return jdbcClient.sql("""
                    INSERT INTO user_profile(
                        user_id,
                        display_name,
                        given_name,
                        family_name,
                        email,
                        picture_url
                    )
                    VALUES(
                        :userId,
                        :displayName,
                        :givenName,
                        :familyName,
                        :email,
                        :pictureUrl
                    )
                    RETURNING user_id,
                              created,
                              modified,
                              display_name,
                              given_name,
                              family_name,
                              email,
                              picture_url
                    """)
                .paramSource(dbo)
                .query(UserProfileDbo.class)
                .single();
    }
}