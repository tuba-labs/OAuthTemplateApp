package org.tubalabs.app.users.identity.externalidentity;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalIdentityLinkService {

    private final Clock clock;
    private final UserIdentityRepository userIdentityRepository;
    private final UserLoginRepository userLoginRepository;

    @Transactional
    public void link(@NonNull UUID userId,
                     @NonNull ExternalIdentity externalIdentity,
                     @NonNull String clientIp,
                     @NonNull String userAgent) {
        if (userIdentityRepository.findByUserIdAndProviderId(userId, externalIdentity.providerId()).isPresent()) {
            throw new IdentityLinkException(IdentityLinkFailure.PROVIDER_ALREADY_LINKED);
        }

        if (userIdentityRepository.findByProviderAndSubject(
                externalIdentity.providerId(),
                externalIdentity.subject()).isPresent()) {
            throw new IdentityLinkException(IdentityLinkFailure.EXTERNAL_IDENTITY_USED);
        }

        userIdentityRepository.insert(newIdentity(userId, externalIdentity));
        insertLogin(userId, externalIdentity, clientIp, userAgent);
    }

    private UserIdentityDbo newIdentity(@NonNull UUID userId, @NonNull ExternalIdentity externalIdentity) {
        return UserIdentityDbo.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .providerId(externalIdentity.providerId())
                .subject(externalIdentity.subject())
                .displayName(externalIdentity.displayName())
                .email(externalIdentity.email())
                .pictureUrl(externalIdentity.avatarUrl())
                .build();
    }

    private void insertLogin(@NonNull UUID userId,
                             @NonNull ExternalIdentity externalIdentity,
                             @NonNull String clientIp,
                             @NonNull String userAgent) {
        userLoginRepository.insert(UserLoginDbo.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .loginTime(Timestamp.from(clock.instant()))
                .providerId(externalIdentity.providerId())
                .subject(externalIdentity.subject())
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build());
    }
}
