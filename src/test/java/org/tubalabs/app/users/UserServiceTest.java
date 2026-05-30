package org.tubalabs.app.users;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.logins.UserIdentityAuditLog;
import org.tubalabs.app.users.profile.UserProfileService;
import org.tubalabs.app.users.user.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";
    private static final String DISPLAY_NAME = "Person";
    private static final String EMAIL = "person@example.com";
    private static final String PICTURE_URL = "https://example.com/avatar.png";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String USER_AGENT = "JUnit";
    private static final ExternalIdentity EXTERNAL_IDENTITY = ExternalIdentity.builder()
            .providerId(PROVIDER_ID)
            .subject(SUBJECT)
            .displayName(DISPLAY_NAME)
            .email(EMAIL)
            .avatarUrl(PICTURE_URL)
            .build();

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final UserIdentityRepository userIdentityRepository = Mockito.mock(UserIdentityRepository.class);
    private final UserProfileService userProfileService = Mockito.mock(UserProfileService.class);
    private final UserIdentityAuditLog userIdentityAuditLog = Mockito.mock(UserIdentityAuditLog.class);
    private final UserService userService = new UserService(
            userRepository,
            userIdentityRepository,
            userProfileService,
            userIdentityAuditLog);

    @Test
    void createsNewUserWithExternalIdentity() {
        when(userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT)).thenReturn(Optional.empty());
        when(userIdentityRepository.insert(Mockito.any())).thenAnswer(invocation -> {
            final UserIdentityDbo identity = invocation.getArgument(0);
            return identity.toBuilder()
                    .id(IDENTITY_ID)
                    .build();
        });

        final LoginResult loginResult = userService.login(EXTERNAL_IDENTITY, CLIENT_IP, USER_AGENT);

        assertThat(loginResult.newUser()).isTrue();
        assertThat(loginResult.providerId()).isEqualTo(PROVIDER_ID);
        assertThat(loginResult.subject()).isEqualTo(SUBJECT);
    }

    @Test
    void updatesExistingUserIdentity() {
        final UserIdentityDbo existingIdentity = UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .displayName(DISPLAY_NAME)
                .email(EMAIL)
                .pictureUrl(PICTURE_URL)
                .build();
        when(userIdentityRepository.findByProviderAndSubject(PROVIDER_ID, SUBJECT))
                .thenReturn(Optional.of(existingIdentity));
        when(userIdentityRepository.update(Mockito.any())).thenReturn(existingIdentity);

        final LoginResult loginResult = userService.login(EXTERNAL_IDENTITY, CLIENT_IP, USER_AGENT);

        assertThat(loginResult.newUser()).isFalse();
        assertThat(loginResult.userId()).isEqualTo(USER_ID);
    }
}
