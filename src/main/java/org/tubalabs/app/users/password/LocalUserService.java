package org.tubalabs.app.users.password;

import jakarta.validation.Valid;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.identity.UserIdentityAlreadyExistsException;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.login.db.UserLoginDbo;
import org.tubalabs.app.users.login.db.UserLoginRepository;
import org.tubalabs.app.users.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.password.db.UserPasswordCredentialRepository;
import org.tubalabs.app.users.password.validation.vetoers.CreationVetoResult;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;
import org.tubalabs.app.users.password.validation.vetoers.UserCreateVetoerService;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class LocalUserService {

    public static final String LOCAL_PROVIDER_ID = "local";

    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final LocalEmailNormalizer emailNormalizer;
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserLoginRepository userLoginRepository;
    private final UserProfileService userProfileService;
    private final UserPasswordCredentialRepository userPasswordCredentialRepository;
    private final UserCreateVetoerService userCreateVetoerService;


    @Transactional
    public CreateResult register(@Valid @NonNull LocalUserRegistration registration) {
        final List<CreationVetoResult> vetoes = userCreateVetoerService.getVetoes(registration);
        if (!vetoes.isEmpty()) {
            return CreateResult.vetoed(vetoes);
        }

        final String normalizedEmail = emailNormalizer.normalize(registration.email());
        final String displayName = optionalTrimmed(registration.displayName());

        final UUID userId = UUID.randomUUID();
        final UUID identityId = UUID.randomUUID();

        @Cleanup
        MDC.MDCCloseable ignore = MDC.putCloseable("userId", userId.toString());

        log.info("Creating new local user");

        userRepository.insert(new UserDbo(userId));
        insertLocalIdentity(identityId, userId, normalizedEmail, displayName);
        userProfileService.createInitialProfile(userId, displayName, normalizedEmail, null);
        userPasswordCredentialRepository.insert(UserPasswordCredentialDbo.builder()
                .userId(userId)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(registration.password()))
                .build());

        return CreateResult.created(userId);
    }

    @Transactional
    public LoginResult login(@NonNull String email,
                             @NonNull String password,
                             @NonNull String clientIp,
                             @NonNull String userAgent) {
        final String normalizedEmail = emailNormalizer.normalize(email);
        final UserPasswordCredentialDbo credential = userPasswordCredentialRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, credential.passwordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return login(normalizedEmail, clientIp, userAgent);
    }

    @Transactional
    public LoginResult login(@NonNull String email, @NonNull String clientIp, @NonNull String userAgent) {
        final String normalizedEmail = emailNormalizer.normalize(email);
        final UserIdentityDbo identity = userIdentityRepository
                .findByProviderAndSubject(LOCAL_PROVIDER_ID, normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("Local identity not found: " + normalizedEmail));

        @Cleanup
        MDC.MDCCloseable ignore = MDC.putCloseable("userId", identity.userId().toString());

        userLoginRepository.insert(newLogin(
                UUID.randomUUID(),
                identity.userId(),
                clock.instant(),
                LOCAL_PROVIDER_ID,
                normalizedEmail,
                clientIp,
                userAgent));

        return LoginResult.builder()
                .userId(identity.userId())
                .providerId(LOCAL_PROVIDER_ID)
                .subject(normalizedEmail)
                .newUser(false)
                .build();
    }

    private void insertLocalIdentity(UUID identityId,
                                     UUID userId,
                                     String normalizedEmail,
                                     String displayName) {
        try {
            userIdentityRepository.insert(UserIdentityDbo.builder()
                    .id(identityId)
                    .userId(userId)
                    .providerId(LOCAL_PROVIDER_ID)
                    .subject(normalizedEmail)
                    .displayName(displayName)
                    .email(normalizedEmail)
                    .build());
        } catch (UserIdentityAlreadyExistsException exception) {
            throw new LocalUserAlreadyExistsException(normalizedEmail, exception);
        }
    }

    private String optionalTrimmed(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
