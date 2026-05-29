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
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.navigation.ui.AbstractNavigationController;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.ui.profile.logintypes.menusystem.ProfileLoginTypesPage;
import org.tubalabs.app.ui.profile.logintypes.menusystem.ProfileLoginTypesPageModel;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserRequestContext;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.CurrentLoginProviderResolver;
import org.tubalabs.app.users.identity.LoginTypeUnlinkException;
import org.tubalabs.app.users.identity.UserLoginTypeService;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentityLinkSession;
import org.tubalabs.app.users.identity.password.LocalUserAlreadyExistsException;
import org.tubalabs.app.users.identity.password.LocalUserRegistration;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.preferences.global.ui.GlobalUserPreferencesPageModel;
import org.tubalabs.app.users.profile.config.ProfileSetupSession;
import org.tubalabs.app.ui.profile.logintypes.dtos.UserLocalLoginTypeLinkDto;
import org.tubalabs.app.ui.profile.logintypes.local.ProfileLocalLoginTypePage;

import java.util.Objects;
import java.util.UUID;

@Controller
public class UserProfileLoginTypesController extends AbstractNavigationController {

    private static final String LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE = "localLoginTypeForm";
    private static final String PROVIDER_LINK_URL = ProfileLoginTypesPage.RELATIVE_URL + "/{providerId}/link";
    private static final String PROVIDER_UNLINK_URL = ProfileLoginTypesPage.RELATIVE_URL + "/{providerId}/unlink";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final CurrentLoginProviderResolver currentLoginProviderResolver;
    private final LocalUserService localUserService;
    private final ProfileSetupSession profileSetupSession;
    private final UserLoginTypeService userLoginTypeService;
    private final ExternalIdentityLinkSession externalIdentityLinkSession;
    private final ProfileLoginTypesPageModel profileLoginTypesPageModel;
    private final LocalizationService localizationService;
    private final CurrentUserSession currentUserSession;

    public UserProfileLoginTypesController(@NonNull CurrentUserIdResolver currentUserIdResolver,
                                           @NonNull CurrentUserRequestContext currentUserRequestContext,
                                           @NonNull CurrentUserSession currentUserSession,
                                           @NonNull NavigationPageModel navigationPageModel,
                                           @NonNull GlobalUserPreferencesPageModel globalUserPreferencesPageModel,
                                           @NonNull CurrentLoginProviderResolver currentLoginProviderResolver,
                                           @NonNull LocalUserService localUserService,
                                           @NonNull ProfileSetupSession profileSetupSession,
                                           @NonNull UserLoginTypeService userLoginTypeService,
                                           @NonNull ExternalIdentityLinkSession externalIdentityLinkSession,
                                           @NonNull ProfileLoginTypesPageModel profileLoginTypesPageModel,
                                           @NonNull LocalizationService localizationService) {
        super(currentUserRequestContext, navigationPageModel, globalUserPreferencesPageModel);
        this.currentUserIdResolver = currentUserIdResolver;
        this.currentLoginProviderResolver = currentLoginProviderResolver;
        this.localUserService = localUserService;
        this.profileSetupSession = profileSetupSession;
        this.userLoginTypeService = userLoginTypeService;
        this.externalIdentityLinkSession = externalIdentityLinkSession;
        this.profileLoginTypesPageModel = profileLoginTypesPageModel;
        this.localizationService = localizationService;
        this.currentUserSession = currentUserSession;
    }

    @GetMapping(ProfileLoginTypesPage.RELATIVE_URL)
    public String loginTypes(@NonNull Authentication authentication,
                             @NonNull Model model,
                             @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final CurrentUser currentUser = currentUser(request, authentication);
        profileLoginTypesPageModel.addLoginTypeAttributes(
                model, currentUser, request, currentLoginProviderResolver.providerId(authentication, request));
        return ProfileLoginTypesPage.VIEW;
    }

    @GetMapping(ProfileLocalLoginTypePage.RELATIVE_URL)
    public String localLoginType(@NonNull Authentication authentication,
                                 @NonNull Model model,
                                 @NonNull HttpServletRequest request) {
        requireCompleteProfile(
                request,
                localizationService.message("profile.login-types.access.link-requires-complete-profile"));

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!userLoginTypeService.canLinkLocalLoginType(userId)) {
            throw new AccessDeniedException(
                    localizationService.message("profile.login-types.access.local-login-unavailable"));
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
        requireCompleteProfile(
                request,
                localizationService.message("profile.login-types.access.link-requires-complete-profile"));

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!userLoginTypeService.canLinkExternalLoginType(userId, providerId)) {
            throw new AccessDeniedException(
                    localizationService.message("profile.login-types.access.external-login-unavailable"));
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
        requireCompleteProfile(
                request,
                localizationService.message("profile.login-types.access.link-requires-complete-profile"));

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!userLoginTypeService.canLinkLocalLoginType(userId)) {
            throw new AccessDeniedException(
                    localizationService.message("profile.login-types.access.local-login-unavailable"));
        }

        if (!Objects.equals(localLoginTypeForm.password(), localLoginTypeForm.passwordConfirmation())) {
            bindingResult.rejectValue(
                    "passwordConfirmation",
                    "passwordMismatch",
                    localizationService.message("profile.login-types.error.password-mismatch"));
        }
        if (bindingResult.hasErrors()) {
            return localLoginTypeViewWithErrors(model, localLoginTypeForm);
        }

        try {
            localUserService.linkLogin(
                    userId,
                    new LocalUserRegistration(localLoginTypeForm.email(), localLoginTypeForm.password()),
                    clientIp(request),
                    userAgent(request));
        } catch (LocalUserAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "email",
                    "localLoginExists",
                    localizationService.message("profile.login-types.error.local-email-used"));
            return localLoginTypeViewWithErrors(model, localLoginTypeForm);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("localLoginRejected", exception.getMessage());
            return localLoginTypeViewWithErrors(model, localLoginTypeForm);
        }

        currentUserSession.refresh(request, userId, false);
        redirectAttributes.addFlashAttribute(
                ProfileLoginTypesPageModel.LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE,
                localizationService.message("profile.login-types.message.local-linked"));
        return ProfileLoginTypesPage.REDIRECT;
    }

    @PostMapping(PROVIDER_UNLINK_URL)
    public String unlinkLoginType(@NonNull Authentication authentication,
                                  @PathVariable @NonNull String providerId,
                                  @NonNull HttpServletRequest request,
                                  @NonNull RedirectAttributes redirectAttributes) {
        requireCompleteProfile(
                request,
                localizationService.message("profile.login-types.access.unlink-requires-complete-profile"));

        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        try {
            userLoginTypeService.unlinkLoginType(
                    userId,
                    providerId,
                    currentLoginProviderResolver.providerId(authentication, request));
            currentUserSession.refresh(request, userId, false);
            redirectAttributes.addFlashAttribute(
                    ProfileLoginTypesPageModel.LOGIN_TYPE_LINKED_MESSAGE_ATTRIBUTE,
                    localizationService.message("profile.login-types.message.unlinked"));
        } catch (LoginTypeUnlinkException exception) {
            redirectAttributes.addFlashAttribute(
                    ProfileLoginTypesPageModel.LOGIN_TYPE_ERROR_MESSAGE_ATTRIBUTE,
                    localizationService.message(exception.reason()));
        }
        return ProfileLoginTypesPage.REDIRECT;
    }

    private String localLoginTypeViewWithErrors(@NonNull Model model,
                                                @NonNull UserLocalLoginTypeLinkDto localLoginTypeForm) {
        model.addAttribute(LOCAL_LOGIN_TYPE_FORM_ATTRIBUTE, localLoginTypeForm);
        return ProfileLocalLoginTypePage.VIEW;
    }

    private void requireCompleteProfile(@NonNull HttpServletRequest request, @NonNull String message) {
        if (profileSetupSession.isProfileSetupRequired(request)) {
            throw new AccessDeniedException(message);
        }
    }

    private String clientIp(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }
}
