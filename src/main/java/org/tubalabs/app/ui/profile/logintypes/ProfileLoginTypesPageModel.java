package org.tubalabs.app.ui.profile.logintypes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.identity.LinkedLoginType;
import org.tubalabs.app.users.identity.UserLoginTypeService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.externalidentity.IdentityLinkFailure;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.ui.profile.logintypes.dtos.ProfileLinkedLoginTypeOptionDto;
import org.tubalabs.app.ui.profile.logintypes.dtos.ProfileLoginTypeOptionDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileLoginTypesPageModel {

    private static final String LOCAL_LOGIN_TYPE_LABEL = "Email and password";
    private static final String CURRENT_LOGIN_STATUS = "Current login";
    private static final String ONLY_LOGIN_TYPE_STATUS = "Only login type";
    private static final String UNKNOWN_CURRENT_LOGIN_STATUS = "Log in again to unlink";
    private static final String LOGIN_TYPE_LINKED_MESSAGE = "Login type linked.";
    private static final String PROVIDER_MISMATCH_MESSAGE = "Could not link the selected login type. Try again.";
    private static final String EXTERNAL_IDENTITY_USED_MESSAGE = "That login type is already linked to another account";
    private static final String PROVIDER_ALREADY_LINKED_MESSAGE = "This account already has that login type linked";
    private static final String NO_STATUS = "";

    public static final String LOGIN_TYPE_OPTIONS_ATTRIBUTE = "loginTypeOptions";
    public static final String LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE = "linkedLoginTypeOptions";
    public static final String LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE = "localLoginTypeAvailable";
    public static final String LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE = "loginTypeLinkedMessage";
    public static final String LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE = "loginTypeErrorMessage";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserLoginTypeService userLoginTypeService;
    private final ExternalIdentityLinkSession externalIdentityLinkSession;

    void addLoginTypeAttributes(@NonNull Model model,
                                @NonNull CurrentUser currentUser,
                                @NonNull HttpServletRequest request,
                                @NonNull Optional<String> currentProviderId) {
        model.addAttribute(LOCAL_LOGIN_TYPE_AVAILABLE_ATTRIBUTE, currentUser.localLoginLinkAvailable());
        model.addAttribute(LOGIN_TYPE_OPTIONS_ATTRIBUTE, availableExternalLoginTypeOptions(currentUser.userId()));
        model.addAttribute(
                LINKED_LOGIN_TYPE_OPTIONS_ATTRIBUTE,
                linkedLoginTypeOptions(currentUser.userId(), currentProviderId));
        if (externalIdentityLinkSession.consumeSuccess(request)) {
            model.addAttribute(LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE, LOGIN_TYPE_LINKED_MESSAGE);
        }
        externalIdentityLinkSession.consumeFailure(request)
                .map(this::identityLinkFailureMessage)
                .ifPresent(message -> model.addAttribute(LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE, message));
    }

    private List<ProfileLoginTypeOptionDto> availableExternalLoginTypeOptions(@NonNull UUID userId) {
        return userLoginTypeService.availableExternalLoginTypes(userId)
                .stream()
                .map(providerId -> new ProfileLoginTypeOptionDto(providerId, label(providerId)))
                .toList();
    }

    private List<ProfileLinkedLoginTypeOptionDto> linkedLoginTypeOptions(@NonNull UUID userId,
                                                                         @NonNull Optional<String> currentProviderId) {
        return userLoginTypeService.linkedLoginTypes(userId, currentProviderId)
                .stream()
                .map(loginType -> new ProfileLinkedLoginTypeOptionDto(
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
