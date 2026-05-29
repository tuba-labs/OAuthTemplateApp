package org.tubalabs.app.users.identity.externalidentity;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.logins.UserIdentityAuditLog;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalIdentityLinkService {

    private final UserIdentityRepository userIdentityRepository;
    private final UserIdentityAuditLog userIdentityAuditLog;

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

        final UserIdentityDbo identity = userIdentityRepository.insert(newIdentity(userId, externalIdentity));
        userIdentityAuditLog.recordLoginRow(identity, clientIp, userAgent);
        userIdentityAuditLog.recordSignInMethodLinked(identity, clientIp, userAgent);
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

}
