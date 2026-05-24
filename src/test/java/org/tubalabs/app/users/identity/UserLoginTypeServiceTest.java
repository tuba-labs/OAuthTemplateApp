package org.tubalabs.app.users.identity;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class UserLoginTypeServiceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String GOOGLE_PROVIDER_ID = "google";
    private static final String GOOGLE_LABEL = "Google";
    private static final String GITHUB_PROVIDER_ID = "github";
    private static final String GITHUB_LABEL = "GitHub";

    private final UserIdentityRepository userIdentityRepository = Mockito.mock(UserIdentityRepository.class);
    private final UserPasswordCredentialRepository userPasswordCredentialRepository =
            Mockito.mock(UserPasswordCredentialRepository.class);
    private final UserLoginTypeService userLoginTypeService = new UserLoginTypeService(
            new InMemoryClientRegistrationRepository(
                    clientRegistration(GOOGLE_PROVIDER_ID, GOOGLE_LABEL),
                    clientRegistration(GITHUB_PROVIDER_ID, GITHUB_LABEL)),
            userIdentityRepository,
            userPasswordCredentialRepository);

    @Test
    void returnsOnlyUnlinkedExternalLoginTypes() {
        when(userIdentityRepository.findByUserId(USER_ID)).thenReturn(List.of(linkedIdentity(GOOGLE_PROVIDER_ID)));

        assertThat(userLoginTypeService.availableExternalLoginTypes(USER_ID))
                .containsExactly(GITHUB_PROVIDER_ID);
        assertThat(userLoginTypeService.canLinkExternalLoginType(USER_ID, GITHUB_PROVIDER_ID)).isTrue();
        assertThat(userLoginTypeService.canLinkExternalLoginType(USER_ID, GOOGLE_PROVIDER_ID)).isFalse();
    }

    @Test
    void returnsWhetherLocalLoginTypeCanBeLinked() {
        when(userIdentityRepository.findByUserIdAndProviderId(USER_ID, LocalUserService.LOCAL_PROVIDER_ID))
                .thenReturn(Optional.empty());

        assertThat(userLoginTypeService.canLinkLocalLoginType(USER_ID)).isTrue();

        when(userIdentityRepository.findByUserIdAndProviderId(USER_ID, LocalUserService.LOCAL_PROVIDER_ID))
                .thenReturn(Optional.of(linkedIdentity(LocalUserService.LOCAL_PROVIDER_ID)));

        assertThat(userLoginTypeService.canLinkLocalLoginType(USER_ID)).isFalse();
    }

    @Test
    void returnsLinkedLoginTypesWithCurrentLoginMarked() {
        when(userIdentityRepository.findByUserId(USER_ID)).thenReturn(List.of(
                linkedIdentity(GOOGLE_PROVIDER_ID),
                linkedIdentity(LocalUserService.LOCAL_PROVIDER_ID)));

        assertThat(userLoginTypeService.linkedLoginTypes(USER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID)))
                .containsExactly(
                        new LinkedLoginType(GOOGLE_PROVIDER_ID, false, LoginTypeUnlinkState.AVAILABLE),
                        new LinkedLoginType(
                                LocalUserService.LOCAL_PROVIDER_ID, true, LoginTypeUnlinkState.CURRENT_LOGIN));
    }

    @Test
    void blocksUnlinkWhenCurrentLoginTypeIsUnknown() {
        when(userIdentityRepository.findByUserId(USER_ID)).thenReturn(List.of(
                linkedIdentity(GOOGLE_PROVIDER_ID),
                linkedIdentity(LocalUserService.LOCAL_PROVIDER_ID)));

        assertThat(userLoginTypeService.linkedLoginTypes(USER_ID, Optional.empty()))
                .containsExactly(
                        new LinkedLoginType(GOOGLE_PROVIDER_ID, false, LoginTypeUnlinkState.UNKNOWN_CURRENT_LOGIN),
                        new LinkedLoginType(
                                LocalUserService.LOCAL_PROVIDER_ID,
                                false,
                                LoginTypeUnlinkState.UNKNOWN_CURRENT_LOGIN));
    }

    @Test
    void unlinksExternalLoginType() {
        when(userIdentityRepository.findByUserId(USER_ID)).thenReturn(List.of(
                linkedIdentity(GOOGLE_PROVIDER_ID),
                linkedIdentity(LocalUserService.LOCAL_PROVIDER_ID)));

        userLoginTypeService.unlinkLoginType(USER_ID, GOOGLE_PROVIDER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID));

        verify(userIdentityRepository).deleteByUserIdAndProviderId(USER_ID, GOOGLE_PROVIDER_ID);
    }

    @Test
    void unlinksLocalLoginTypeAndPasswordCredential() {
        when(userIdentityRepository.findByUserId(USER_ID)).thenReturn(List.of(
                linkedIdentity(GOOGLE_PROVIDER_ID),
                linkedIdentity(LocalUserService.LOCAL_PROVIDER_ID)));

        userLoginTypeService.unlinkLoginType(USER_ID, LocalUserService.LOCAL_PROVIDER_ID, Optional.of(GOOGLE_PROVIDER_ID));

        verify(userPasswordCredentialRepository).deleteByUserId(USER_ID);
        verify(userIdentityRepository).deleteByUserIdAndProviderId(USER_ID, LocalUserService.LOCAL_PROVIDER_ID);
    }

    @Test
    void rejectsUnlinkingCurrentLoginType() {
        assertThatExceptionOfType(LoginTypeUnlinkException.class)
                .isThrownBy(() -> userLoginTypeService.unlinkLoginType(
                USER_ID, GOOGLE_PROVIDER_ID, Optional.of(GOOGLE_PROVIDER_ID)))
                .satisfies(exception -> assertThat(exception.reason()).isEqualTo(LoginTypeUnlinkFailure.CURRENT_LOGIN));
    }

    @Test
    void rejectsUnlinkingLastLoginType() {
        when(userIdentityRepository.findByUserId(USER_ID)).thenReturn(List.of(linkedIdentity(GOOGLE_PROVIDER_ID)));

        assertThatExceptionOfType(LoginTypeUnlinkException.class)
                .isThrownBy(() -> userLoginTypeService.unlinkLoginType(
                USER_ID, GOOGLE_PROVIDER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID)))
                .satisfies(exception -> assertThat(exception.reason()).isEqualTo(LoginTypeUnlinkFailure.LAST_LOGIN_TYPE));
    }

    @Test
    void rejectsUnlinkingLoginTypeThatIsNotLinked() {
        when(userIdentityRepository.findByUserId(USER_ID)).thenReturn(List.of(
                linkedIdentity(GOOGLE_PROVIDER_ID),
                linkedIdentity(LocalUserService.LOCAL_PROVIDER_ID)));

        assertThatExceptionOfType(LoginTypeUnlinkException.class)
                .isThrownBy(() -> userLoginTypeService.unlinkLoginType(
                        USER_ID, GITHUB_PROVIDER_ID, Optional.of(LocalUserService.LOCAL_PROVIDER_ID)))
                .satisfies(exception -> assertThat(exception.reason()).isEqualTo(LoginTypeUnlinkFailure.NOT_LINKED));
        Mockito.verify(userIdentityRepository, Mockito.never()).deleteByUserIdAndProviderId(USER_ID, GITHUB_PROVIDER_ID);
    }

    @Test
    void rejectsUnlinkingWhenCurrentLoginTypeIsUnknown() {
        assertThatExceptionOfType(LoginTypeUnlinkException.class)
                .isThrownBy(() -> userLoginTypeService.unlinkLoginType(
                USER_ID, GOOGLE_PROVIDER_ID, Optional.empty()))
                .satisfies(exception -> assertThat(exception.reason()).isEqualTo(LoginTypeUnlinkFailure.UNKNOWN_CURRENT_LOGIN));
    }

    private UserIdentityDbo linkedIdentity(String providerId) {
        return UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(providerId)
                .subject("subject")
                .build();
    }

    private ClientRegistration clientRegistration(String providerId, String label) {
        return ClientRegistration.withRegistrationId(providerId)
                .clientName(label)
                .clientId("client-" + providerId)
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/" + providerId)
                .authorizationUri("https://example.com/" + providerId + "/authorize")
                .tokenUri("https://example.com/" + providerId + "/token")
                .build();
    }
}
