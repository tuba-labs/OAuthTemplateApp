package org.tubalabs.app.users.db.login;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.security.identity.ExternalIdentity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserLoginMapper {

    public UserLoginDbo newLogin(
            @NonNull UUID loginId,
            @NonNull UUID userId,
            @NonNull Instant loginTime,
            Optional<ExternalIdentity> externalIdentity,
            @NonNull String clientIp,
            @NonNull String userAgent) {

        return UserLoginDbo.builder()
                .id(loginId)
                .userId(userId)
                .loginTime(Timestamp.from(loginTime))
                .providerId(externalIdentity.map(ExternalIdentity::providerId).orElse(""))
                .subject(externalIdentity.map(ExternalIdentity::subject).orElse(userId.toString()))
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
    }


}