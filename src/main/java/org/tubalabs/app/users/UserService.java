package org.tubalabs.app.users;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.events.EventLogService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final Clock clock;

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserLoginRepository userLoginRepository;

    private final UserProfileService userProfileService;
    private final EventLogService eventLogService;
    private final UserIdentityEventFactory userIdentityEventFactory;

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

        userLoginRepository.insert(newLogin(
                UUID.randomUUID(),
                userId,
                clock.instant(),
                externalIdentity.providerId(),
                externalIdentity.subject(),
                clientIp,
                userAgent));
        eventLogService.record(userIdentityEventFactory.login(identity, clientIp, userAgent, false, true));

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

        userLoginRepository.insert(newLogin(
                UUID.randomUUID(),
                localIdentity.userId(),
                clock.instant(),
                externalIdentity.providerId(),
                externalIdentity.subject(),
                clientIp,
                userAgent));
        eventLogService.record(userIdentityEventFactory.login(localIdentity, clientIp, userAgent, false, false));

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

    private UserLoginDbo newLogin(
            UUID loginId,
            UUID userId,
            Instant loginTime,
            String providerId,
            String subject,
            String clientIp,
            String userAgent) {

        return UserLoginDbo.builder()
                .id(loginId)
                .userId(userId)
                .loginTime(Timestamp.from(loginTime))
                .providerId(providerId)
                .subject(subject)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .build();
    }

}
