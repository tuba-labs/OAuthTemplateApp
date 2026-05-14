package org.tubalabs.app.users.db;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.security.identity.ExternalIdentity;
import org.tubalabs.app.users.db.identity.UserIdentityDbo;
import org.tubalabs.app.users.db.identity.UserIdentityMapper;
import org.tubalabs.app.users.db.identity.UserIdentityRepository;
import org.tubalabs.app.users.db.login.UserLoginMapper;
import org.tubalabs.app.users.db.login.UserLoginRepository;
import org.tubalabs.app.users.db.profile.UserProfileService;
import org.tubalabs.app.users.db.user.UserMapper;
import org.tubalabs.app.users.db.user.UserRepository;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final Clock clock;

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserLoginRepository userLoginRepository;

    private final UserMapper userMapper;
    private final UserIdentityMapper userIdentityMapper;
    private final UserLoginMapper userLoginMapper;
    private final UserProfileService userProfileService;

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

        userRepository.insert(userMapper.newUser(userId));

        userIdentityRepository.insert(
                userIdentityMapper.newIdentity(
                        identityId,
                        userId,
                        externalIdentity));

        userProfileService.createInitialProfile(
                userId,
                externalIdentity);

        userLoginRepository.insert(
                userLoginMapper.newLogin(
                        UUID.randomUUID(),
                        userId,
                        clock.instant(),
                        Optional.of(externalIdentity),
                        clientIp,
                        userAgent));

        return LoginResult.builder()
                .userId(userId)
                .providerId(externalIdentity.providerId())
                .subject(externalIdentity.subject())
                .build();
    }

    private LoginResult existing(ExternalIdentity externalIdentity,
                                 UserIdentityDbo localIdentity,
                                 String clientIp,
                                 String userAgent) {
        @Cleanup
        MDC.MDCCloseable ignore = MDC.putCloseable("userId", localIdentity.userId().toString());

        log.debug("Found existing identity");

        final UserIdentityDbo updatedIdentity = userIdentityMapper.updateIdentity(
                localIdentity, externalIdentity);

        userIdentityRepository.update(updatedIdentity);

        userLoginRepository.insert(userLoginMapper.newLogin(
                UUID.randomUUID(), localIdentity.userId(), clock.instant(),
                Optional.of(externalIdentity), clientIp, userAgent));

        return LoginResult.builder()
                .userId(localIdentity.userId())
                .providerId(localIdentity.providerId())
                .subject(localIdentity.subject())
                .build();
    }
}