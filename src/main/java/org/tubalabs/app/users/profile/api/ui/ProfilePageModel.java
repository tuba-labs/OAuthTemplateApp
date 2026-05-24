package org.tubalabs.app.users.profile.api.ui;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tubalabs.app.users.identity.LinkedLoginType;
import org.tubalabs.app.users.identity.UserLoginTypeService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.externalidentity.IdentityLinkFailure;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProfilePageModel {

    private static final String LOCAL_LOGIN_TYPE_LABEL = "Email and password";
    private static final String CURRENT_LOGIN_STATUS = "Current login";
    private static final String ONLY_LOGIN_TYPE_STATUS = "Only login type";
    private static final String UNKNOWN_CURRENT_LOGIN_STATUS = "Log in again to unlink";
    private static final String LOGIN_TYPE_LINKED_MESSAGE = "Login type linked.";
    private static final String PROVIDER_MISMATCH_MESSAGE = "Could not link the selected login type. Try again.";
    private static final String EXTERNAL_IDENTITY_USED_MESSAGE = "That login type is already linked to another account";
    private static final String PROVIDER_ALREADY_LINKED_MESSAGE = "This account already has that login type linked";
    private static final String NO_STATUS = "";

    static final String LOCAL_PROFILE_ATTRIBUTE = "localProfile";
    static final String LOCAL_LOGIN_NAME_ATTRIBUTE = "localLoginName";
    static final String PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE = "passwordChangeAvailable";
    static final String LOGIN_TYPES_AVAILABLE_ATTRIBUTE = "loginTypesAvailable";
    static final String LOGIN_TYPE_OPTIONS_ATTRIBUTE = "loginTypeOptions";
    static final String LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE = "linkedLoginTypeOptions";
    static final String LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE = "localLoginTypeAvailable";
    static final String LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE = "loginTypeLinkedMessage";
    static final String LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE = "loginTypeErrorMessage";
    static final String PROFILE_PICTURE_ATTRIBUTE = "profilePictureUrl";

    private final LocalUserService localUserService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ProfileSetupSession profileSetupSession;
    private final UserLoginTypeService userLoginTypeService;
    private final ExternalIdentityLinkSession externalIdentityLinkSession;

    void addProfileAttributes(@NonNull Model model,
                              @NonNull UUID userId,
                              @NonNull UserProfileDbo profile,
                              @NonNull HttpServletRequest request) {
        addMenuAttributes(model, userId, request);
        model.addAttribute(PROFILE_PICTURE_ATTRIBUTE, profile.pictureUrl());
    }

    void addMenuAttributes(@NonNull Model model, @NonNull UUID userId, @NonNull HttpServletRequest request) {
        addMenuAttributes(model, request, localUserService.loginName(userId));
    }

    void addMenuAttributes(@NonNull Model model,
                           @NonNull HttpServletRequest request,
                           @NonNull Optional<String> loginName) {
        final boolean profileSetupRequired = profileSetupSession.isProfileSetupRequired(request);
        model.addAttribute(LOCAL_PROFILE_ATTRIBUTE, loginName.isPresent());
        model.addAttribute(PASSWORD_CHANGE_AVAILABLE_ATTRIBUTE, loginName.isPresent() && !profileSetupRequired);
        model.addAttribute(LOGIN_TYPES_AVAILABLE_ATTRIBUTE, !profileSetupRequired);
        loginName.ifPresent(value -> model.addAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE, value));
    }

    void addLoginTypeAttributes(@NonNull Model model,
                                @NonNull UUID userId,
                                @NonNull HttpServletRequest request,
                                @NonNull Optional<String> currentProviderId) {
        final boolean profileSetupRequired = profileSetupSession.isProfileSetupRequired(request);
        final boolean loginTypesAvailable = !profileSetupRequired;
        final boolean localLoginTypeAvailable = loginTypesAvailable && userLoginTypeService.canLinkLocalLoginType(userId);
        model.addAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE, localLoginTypeAvailable);
        model.addAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE, loginTypesAvailable
                ? availableExternalLoginTypeOptions(userId)
                : List.of());
        model.addAttribute(LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE, loginTypesAvailable
                ? linkedLoginTypeOptions(userId, currentProviderId)
                : List.of());
        if (externalIdentityLinkSession.consumeSuccess(request)) {
            model.addAttribute(LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE, LOGIN_TYPE_LINKED_MESSAGE);
        }
        externalIdentityLinkSession.consumeFailure(request)
                .map(this::identityLinkFailureMessage)
                .ifPresent(message -> model.addAttribute(LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE, message));
    }

    private List<ProfileLoginTypeOption> availableExternalLoginTypeOptions(@NonNull UUID userId) {
        return userLoginTypeService.availableExternalLoginTypes(userId)
                .stream()
                .map(providerId -> new ProfileLoginTypeOption(providerId, label(providerId)))
                .toList();
    }

    private List<ProfileLinkedLoginTypeOption> linkedLoginTypeOptions(@NonNull UUID userId,
                                                                      @NonNull Optional<String> currentProviderId) {
        return userLoginTypeService.linkedLoginTypes(userId, currentProviderId)
                .stream()
                .map(loginType -> new ProfileLinkedLoginTypeOption(
                        loginType.providerId(),
                        label(loginType.providerId()),
                        loginType.current(),
                        loginType.unlinkAvailable(),
                        status(loginType)))
                .toList();
    }

    private String label(String providerId) {
        if (LocalUserService.LOCAL_PROVIDER_ID.equals(providerId)) {
            return LOCAL_LOGIN_TYPE_LABEL;
        }
        final ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(providerId);
        if (clientRegistration == null) {
            return providerId;
        }
        return clientRegistration.getClientName();
    }

    private String status(LinkedLoginType loginType) {
        return switch (loginType.unlinkState()) {
            case AVAILABLE -> NO_STATUS;
            case CURRENT_LOGIN -> CURRENT_LOGIN_STATUS;
            case LAST_LOGIN_TYPE -> ONLY_LOGIN_TYPE_STATUS;
            case UNKNOWN_CURRENT_LOGIN -> UNKNOWN_CURRENT_LOGIN_STATUS;
        };
    }

    private String identityLinkFailureMessage(@NonNull IdentityLinkFailure failure) {
        return switch (failure) {
            case PROVIDER_MISMATCH -> PROVIDER_MISMATCH_MESSAGE;
            case EXTERNAL_IDENTITY_USED -> EXTERNAL_IDENTITY_USED_MESSAGE;
            case PROVIDER_ALREADY_LINKED -> PROVIDER_ALREADY_LINKED_MESSAGE;
        };
    }
}
