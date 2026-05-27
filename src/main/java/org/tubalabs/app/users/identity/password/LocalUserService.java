package org.tubalabs.app.users.identity.password;

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
import org.tubalabs.app.events.EventLogService;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.identity.UserIdentityAlreadyExistsException;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialAlreadyExistsException;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialRepository;
import org.tubalabs.app.users.identity.password.validation.vetoers.CreationVetoResult;
import org.tubalabs.app.users.identity.password.validation.vetoers.UserCreateVetoerService;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    private final EventLogService eventLogService;
    private final UserIdentityEventFactory userIdentityEventFactory;

    @Transactional
    public CreateResult register(@Valid @NonNull LocalUserRegistration registration) {
        final List<CreationVetoResult> vetoes = userCreateVetoerService.getVetoes(registration);
        if (!vetoes.isEmpty()) {
            return CreateResult.vetoed(vetoes);
        }

        final String normalizedEmail = emailNormalizer.normalize(registration.email());

        final UUID userId = UUID.randomUUID();
        final UUID identityId = UUID.randomUUID();

        @Cleanup final MDC.MDCCloseable ignore = MDC.putCloseable("userId", userId.toString());

        log.info("Creating new local user");

        userRepository.insert(new UserDbo(userId));
        insertLocalIdentity(identityId, userId, normalizedEmail);
        userProfileService.createInitialProfile(
                userId,
                emailToDisplayName(normalizedEmail));
        insertPasswordCredential(userId, normalizedEmail, registration.password());

        return CreateResult.created(userId);
    }

    private String emailToDisplayName(@NonNull String normalizedEmail) {
        final String localPart = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
        return Character.toUpperCase(localPart.charAt(0)) + localPart.substring(1);
    }

    public Optional<String> loginName(@NonNull UUID userId) {
        return userPasswordCredentialRepository.findByUserId(userId)
                .map(UserPasswordCredentialDbo::email);
    }

    @Transactional
    public void linkLogin(@NonNull UUID userId,
                          @Valid @NonNull LocalUserRegistration registration,
                          @NonNull String clientIp,
                          @NonNull String userAgent) {
        if (userPasswordCredentialRepository.findByUserId(userId).isPresent()
                || userIdentityRepository.findByUserIdAndProviderId(userId, LOCAL_PROVIDER_ID).isPresent()) {
            throw new IllegalArgumentException("This account already has email and password login linked");
        }

        final List<CreationVetoResult> vetoes = userCreateVetoerService.getVetoes(registration);
        if (!vetoes.isEmpty()) {
            throw new IllegalArgumentException(vetoes.get(0).englishReason());
        }

        final String normalizedEmail = emailNormalizer.normalize(registration.email());
        if (userPasswordCredentialRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new LocalUserAlreadyExistsException(normalizedEmail);
        }

        final UserIdentityDbo identity = insertLocalIdentity(UUID.randomUUID(), userId, normalizedEmail);
        insertPasswordCredential(userId, normalizedEmail, registration.password());
        eventLogService.record(userIdentityEventFactory.signInMethodLinked(identity, clientIp, userAgent));
    }

    @Transactional
    public void changePassword(@NonNull UUID userId, @NonNull String currentPassword, @NonNull String newPassword) {
        final UserPasswordCredentialDbo credential = userPasswordCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Local account not found"));

        if (!passwordEncoder.matches(currentPassword, credential.passwordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        final List<CreationVetoResult> vetoes = userCreateVetoerService.getVetoes(
                new LocalUserRegistration(credential.email(), newPassword));
        if (!vetoes.isEmpty()) {
            throw new IllegalArgumentException(vetoes.get(0).englishReason());
        }

        userPasswordCredentialRepository.updatePasswordHash(userId, passwordEncoder.encode(newPassword));
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
        eventLogService.record(userIdentityEventFactory.login(identity, clientIp, userAgent, false, false));

        return LoginResult.builder()
                .identityId(identity.id())
                .userId(identity.userId())
                .providerId(LOCAL_PROVIDER_ID)
                .subject(normalizedEmail)
                .newUser(false)
                .build();
    }

    private UserIdentityDbo insertLocalIdentity(UUID identityId,
                                                UUID userId,
                                                String normalizedEmail) {
        try {
            return userIdentityRepository.insert(UserIdentityDbo.builder()
                    .id(identityId)
                    .userId(userId)
                    .providerId(LOCAL_PROVIDER_ID)
                    .subject(normalizedEmail)
                    .email(normalizedEmail)
                    .build());
        } catch (UserIdentityAlreadyExistsException exception) {
            throw new LocalUserAlreadyExistsException(normalizedEmail, exception);
        }
    }

    private void insertPasswordCredential(@NonNull UUID userId,
                                          @NonNull String normalizedEmail,
                                          @NonNull String password) {
        try {
            userPasswordCredentialRepository.insert(
                    UserPasswordCredentialDbo.builder()
                            .userId(userId)
                            .email(normalizedEmail)
                            .passwordHash(passwordEncoder.encode(password))
                            .build());
        } catch (UserPasswordCredentialAlreadyExistsException exception) {
            throw new LocalUserAlreadyExistsException(normalizedEmail, exception);
        }
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
