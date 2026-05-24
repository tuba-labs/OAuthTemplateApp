package org.tubalabs.app.ui.profile.logintypes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tubalabs.app.navigation.ui.AbstractNavigationController;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.CurrentLoginProviderResolver;
import org.tubalabs.app.users.identity.LoginTypeUnlinkException;
import org.tubalabs.app.users.identity.UserLoginTypeService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.password.LocalUserAlreadyExistsException;
import org.tubalabs.app.users.identity.password.LocalUserRegistration;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.ui.profile.logintypes.dtos.UserLocalLoginTypeLinkDto;
import org.tubalabs.app.ui.profile.logintypes.local.ProfileLocalLoginTypePage;

import java.util.Objects;
import java.util.UUID;

@Controller
public class UserProfileLoginTypesController extends AbstractNavigationController {

    private static final String LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE = "localLoginTypeForm";
    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match";
    private static final String CURRENT_LOGIN_UNLINK_MESSAGE = "You cannot unlink the login type used for this session";
    private static final String LAST_LOGIN_TYPE_UNLINK_MESSAGE = "At least one login type must remain linked";
    private static final String UNKNOWN_CURRENT_LOGIN_UNLINK_MESSAGE = "Log in again before unlinking login types";
    private static final String UNLINKED_LOGIN_TYPE_MESSAGE = "Login type is not linked";
    private static final String PROVIDER_LINK_URL = ProfileLoginTypesPage.RELATIVE_URL + "/{providerId}/link";
    private static final String PROVIDER_UNLINK_URL = ProfileLoginTypesPage.RELATIVE_URL + "/{providerId}/unlink";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final CurrentLoginProviderResolver currentLoginProviderResolver;
    private final LocalUserService localUserService;
    private final ProfileSetupSession profileSetupSession;
    private final UserLoginTypeService userLoginTypeService;
    private final ExternalIdentityLinkSession externalIdentityLinkSession;
    private final ProfileLoginTypesPageModel profileLoginTypesPageModel;
    private final CurrentUserSession currentUserSession;

    public UserProfileLoginTypesController(@NonNull CurrentUserIdResolver currentUserIdResolver,
                                           @NonNull CurrentUserSession currentUserSession,
                                           @NonNull ProfileSetupRequirementService profileSetupRequirementService,
                                           @NonNull NavigationPageModel navigationPageModel,
                                           @NonNull CurrentLoginProviderResolver currentLoginProviderResolver,
                                           @NonNull LocalUserService localUserService,
                                           @NonNull ProfileSetupSession profileSetupSession,
                                           @NonNull UserLoginTypeService userLoginTypeService,
                                           @NonNull ExternalIdentityLinkSession externalIdentityLinkSession,
                                           @NonNull ProfileLoginTypesPageModel profileLoginTypesPageModel) {
        super(currentUserIdResolver, currentUserSession, profileSetupRequirementService, navigationPageModel);
        this.currentUserIdResolver = currentUserIdResolver;
        this.currentLoginProviderResolver = currentLoginProviderResolver;
        this.localUserService = localUserService;
        this.profileSetupSession = profileSetupSession;
        this.userLoginTypeService = userLoginTypeService;
        this.externalIdentityLinkSession = externalIdentityLinkSession;
        this.profileLoginTypesPageModel = profileLoginTypesPageModel;
        this.currentUserSession = currentUserSession;
    }

    @GetMapping(ProfileLoginTypesPage.RELATIVE_URL)
    public String loginTypes(@NonNull Authentication authentication,
                             @NonNull Model model,
                             @NonNull HttpServletRequest request) {
        requireCompleteProfile(request, "A complete profile is required to manage login types");

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final CurrentUser currentUser = currentUser(userId, request);
        profileLoginTypesPageModel.addLoginTypeAttributes(
                model, currentUser, request, currentLoginProviderResolver.providerId(authentication, request));
        return ProfileLoginTypesPage.VIEW;
    }

    @GetMapping(ProfileLocalLoginTypePage.RELATIVE_URL)
    public String localLoginType(@NonNull Authentication authentication,
                                 @NonNull Model model,
                                 @NonNull HttpServletRequest request) {
        requireCompleteProfile(request, "A complete profile is required to link login types");

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!userLoginTypeService.canLinkLocalLoginType(userId)) {
            throw new AccessDeniedException("Email and password login is not available to link");
        }

        if (!model.containsAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE)) {
            model.addAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE, new UserLocalLoginTypeLinkDto(null, null, null));
        }
        return ProfileLocalLoginTypePage.VIEW;
    }

    @PostMapping(PROVIDER_LINK_URL)
    public String linkLoginType(@NonNull Authentication authentication,
                                @PathVariable @NonNull String providerId,
                                @NonNull HttpServletRequest request) {
        requireCompleteProfile(request, "A complete profile is required to link login types");

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!userLoginTypeService.canLinkExternalLoginType(userId, providerId)) {
            throw new AccessDeniedException("Login type is not available to link");
        }

        externalIdentityLinkSession.start(request, userId, providerId, authentication);
        return "redirect:/oauth2/authorization/" + providerId;
    }

    @PostMapping(ProfileLocalLoginTypePage.RELATIVE_URL)
    public String linkLocalLoginType(@NonNull Authentication authentication,
                                     @Valid @ModelAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE) @NonNull UserLocalLoginTypeLinkDto localLoginTypeForm,
                                     @NonNull BindingResult bindingResult,
                                     @NonNull Model model,
                                     @NonNull RedirectAttributes redirectAttributes,
                                     @NonNull HttpServletRequest request) {
        requireCompleteProfile(request, "A complete profile is required to link login types");

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!userLoginTypeService.canLinkLocalLoginType(userId)) {
            throw new AccessDeniedException("Email and password login is not available to link");
        }

        if (!Objects.equals(localLoginTypeForm.password(), localLoginTypeForm.passwordConfirmation())) {
            bindingResult.rejectValue("passwordConfirmation", "passwordMismatch", PASSWORD_MISMATCH_MESSAGE);
        }
        if (bindingResult.hasErrors()) {
            return localLoginTypeViewWithErrors(model, localLoginTypeForm);
        }

        try {
            localUserService.linkLogin(
                    userId,
                    new LocalUserRegistration(localLoginTypeForm.email(), localLoginTypeForm.password()));
        } catch (LocalUserAlreadyExistsException exception) {
            bindingResult.rejectValue("email", "localLoginExists", "That email is already linked to another account");
            return localLoginTypeViewWithErrors(model, localLoginTypeForm);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("localLoginRejected", exception.getMessage());
            return localLoginTypeViewWithErrors(model, localLoginTypeForm);
        }

        currentUserSession.refresh(request, userId, false);
        redirectAttributes.addFlashAttribute(
                ProfileLoginTypesPageModel.LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE,
                "Email and password linked.");
        return ProfileLoginTypesPage.REDIRECT;
    }

    @PostMapping(PROVIDER_UNLINK_URL)
    public String unlinkLoginType(@NonNull Authentication authentication,
                                  @PathVariable @NonNull String providerId,
                                  @NonNull HttpServletRequest request,
                                  @NonNull RedirectAttributes redirectAttributes) {
        requireCompleteProfile(request, "A complete profile is required to unlink login types");

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        try {
            userLoginTypeService.unlinkLoginType(userId, providerId, currentLoginProviderResolver.providerId(authentication, request));
            currentUserSession.refresh(request, userId, false);
            redirectAttributes.addFlashAttribute(
                    ProfileLoginTypesPageModel.LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE,
                    "Login type unlinked.");
        } catch (LoginTypeUnlinkException exception) {
            redirectAttributes.addFlashAttribute(
                    ProfileLoginTypesPageModel.LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE,
                    unlinkFailureMessage(exception));
        }
        return ProfileLoginTypesPage.REDIRECT;
    }

    private String localLoginTypeViewWithErrors(@NonNull Model model,
                                                @NonNull UserLocalLoginTypeLinkDto localLoginTypeForm) {
        model.addAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE, localLoginTypeForm);
        return ProfileLocalLoginTypePage.VIEW;
    }

    private String unlinkFailureMessage(@NonNull LoginTypeUnlinkException exception) {
        return switch (exception.reason()) {
            case CURRENT_LOGIN -> CURRENT_LOGIN_UNLINK_MESSAGE;
            case LAST_LOGIN_TYPE -> LAST_LOGIN_TYPE_UNLINK_MESSAGE;
            case UNKNOWN_CURRENT_LOGIN -> UNKNOWN_CURRENT_LOGIN_UNLINK_MESSAGE;
            case NOT_LINKED -> UNLINKED_LOGIN_TYPE_MESSAGE;
        };
    }

    private void requireCompleteProfile(@NonNull HttpServletRequest request, @NonNull String message) {
        if (profileSetupSession.isProfileSetupRequired(request)) {
            throw new AccessDeniedException(message);
        }
    }

    private CurrentUser currentUser(@NonNull UUID userId, @NonNull HttpServletRequest request) {
        return currentUserSession.currentUser(request)
                .orElseGet(() -> currentUserSession.refresh(
                        request, userId, profileSetupSession.isProfileSetupRequired(request)));
    }
}
