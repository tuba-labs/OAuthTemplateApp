package org.tubalabs.app.users;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.logins.UserIdentityAuditLog;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    private final UserProfileService userProfileService;
    private final UserIdentityAuditLog userIdentityAuditLog;

    @Transactional
    public LoginResult login(ExternalIdentity externalIdentity, String clientIp, String userAgent) {
        return userIdentityRepository
                .findByProviderAndSubject(
                        externalIdentity.providerId(),
                        externalIdentity.subject())
                .map(localIdentity -> existing(externalIdentity, localIdentity, clientIp, userAgent))
                .orElseGet(() -> newUser(externalIdentity, clientIp, userAgent));
    }

    private LoginResult newUser(ExternalIdentity externalIdentity, String clientIp, String userAgent) {
        final UUID userId = UUID.randomUUID();

        final UUID identityId = UUID.randomUUID();

        @Cleanup
        MDC.MDCCloseable ignore = MDC.putCloseable("userId", userId.toString());

        log.info("Creating new user");

        userRepository.insert(new UserDbo(userId));

        final UserIdentityDbo identity = userIdentityRepository.insert(newIdentity(identityId, userId, externalIdentity));

        userProfileService.createInitialProfile(
                userId,
                externalIdentity);

        userIdentityAuditLog.recordLogin(identity, clientIp, userAgent, false, true);

        return LoginResult.builder()
                .identityId(identity.id())
                .userId(userId)
                .providerId(externalIdentity.providerId())
                .subject(externalIdentity.subject())
                .newUser(true)
                .build();
    }

    private LoginResult existing(ExternalIdentity externalIdentity,
                                 UserIdentityDbo localIdentity,
                                 String clientIp,
                                 String userAgent) {
        @Cleanup
        MDC.MDCCloseable ignore = MDC.putCloseable("userId", localIdentity.userId().toString());

        log.debug("Found existing identity");

        final UserIdentityDbo updatedIdentity = localIdentity.toBuilder()
                .displayName(externalIdentity.displayName())
                .email(externalIdentity.email())
                .pictureUrl(externalIdentity.avatarUrl())
                .build();

        userIdentityRepository.update(updatedIdentity);

        userIdentityAuditLog.recordLogin(localIdentity, clientIp, userAgent, false, false);

        return LoginResult.builder()
                .identityId(localIdentity.id())
                .userId(localIdentity.userId())
                .providerId(localIdentity.providerId())
                .subject(localIdentity.subject())
                .newUser(false)
                .build();
    }

    private UserIdentityDbo newIdentity(
            UUID identityId,
            UUID userId,
            ExternalIdentity externalIdentity) {

        return UserIdentityDbo.builder()
                .id(identityId)
                .userId(userId)
                .providerId(externalIdentity.providerId())
                .subject(externalIdentity.subject())
                .displayName(externalIdentity.displayName())
                .email(externalIdentity.email())
                .pictureUrl(externalIdentity.avatarUrl())
                .build();
    }

}
