package org.tubalabs.app.ui.profile.logintypes.menusystem;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.identity.UserLoginTypeService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.ui.profile.logintypes.dtos.ProfileLinkedLoginTypeOptionDto;
import org.tubalabs.app.ui.profile.logintypes.dtos.ProfileLoginTypeOptionDto;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProfileLoginTypesPageModel {

    public static final String LOGIN_TYPE_OPTIONS_ATTRIBUTE = "loginTypeOptions";
    public static final String LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE = "linkedLoginTypeOptions";
    public static final String LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE = "localLoginTypeAvailable";
    public static final String LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE = "loginTypeLinkedMessage";
    public static final String LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE = "loginTypeErrorMessage";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserLoginTypeService userLoginTypeService;
    private final ExternalIdentityLinkSession externalIdentityLinkSession;
    private final LocalizationService localizationService;

    public void addLoginTypeAttributes(@NonNull Model model,
                                @NonNull CurrentUser currentUser,
                                @NonNull HttpServletRequest request,
                                @NonNull Optional<String> currentProviderId) {
        model.addAttribute(
                LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE,
                !currentUser.profileSetupRequired() && currentUser.localLoginLinkAvailable());
        model.addAttribute(
                LOGIN_TYPE_OPTIONS_ATTRIBUTE,
                availableExternalLoginTypeOptions(currentUser));
        model.addAttribute(
                LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE,
                linkedLoginTypeOptions(currentUser, currentProviderId));
        if (externalIdentityLinkSession.consumeSuccess(request)) {
            model.addAttribute(
                    LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE,
                    localizationService.message("profile.login-types.message.linked"));
        }
        externalIdentityLinkSession.consumeFailure(request)
                .map(localizationService::message)
                .ifPresent(message -> model.addAttribute(LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE, message));
    }

    private List<ProfileLoginTypeOptionDto> availableExternalLoginTypeOptions(@NonNull CurrentUser currentUser) {
        if (currentUser.profileSetupRequired()) {
            return List.of();
        }
        return userLoginTypeService.availableExternalLoginTypes(currentUser.userId())
                .stream()
                .map(providerId -> new ProfileLoginTypeOptionDto(providerId, label(providerId)))
                .toList();
    }

    private List<ProfileLinkedLoginTypeOptionDto> linkedLoginTypeOptions(@NonNull CurrentUser currentUser,
                                                                         @NonNull Optional<String> currentProviderId) {
        return userLoginTypeService.linkedLoginTypes(currentUser.userId(), currentProviderId)
                .stream()
                .map(loginType -> new ProfileLinkedLoginTypeOptionDto(
                        loginType.providerId(),
                        label(loginType.providerId()),
                        localLoginType(loginType.providerId()),
                        passwordChangeAvailable(currentUser, loginType.providerId()),
                        loginType.current(),
                        !currentUser.profileSetupRequired() && loginType.unlinkAvailable(),
                        localizationService.message(loginType.unlinkState())))
                .toList();
    }

    private String label(String providerId) {
        if (localLoginType(providerId)) {
            return localizationService.message("profile.login-types.local.label");
        }
        final ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(providerId);
        if (clientRegistration == null) {
            return providerId;
        }
        return clientRegistration.getClientName();
    }

    private boolean localLoginType(String providerId) {
        return LocalUserService.LOCAL_PROVIDER_ID.equals(providerId);
    }

    private boolean passwordChangeAvailable(@NonNull CurrentUser currentUser, @NonNull String providerId) {
        return localLoginType(providerId) && currentUser.passwordChangeAvailable();
    }
}
