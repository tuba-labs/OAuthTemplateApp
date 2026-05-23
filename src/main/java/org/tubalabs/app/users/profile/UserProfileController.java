package org.tubalabs.app.users.profile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserProfileController {

    private static final String PROFILE_FORM_ATTRIBUTE = "profileForm";
    private static final String PROFILE_EMAIL_ATTRIBUTE = "profileEmail";
    private static final String PROFILE_SAVED_ATTRIBUTE = "profileSaved";
    private static final String PROFILE_VIEW = "org/tubalabs/app/users/profile/profile";
    private static final String PROFILE_REDIRECT = "redirect:/profile";
    private static final String REMEMBER_LOGIN_REDIRECT = "redirect:/remember-login";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final UserProfileService userProfileService;
    private final ProfileSetupSession profileSetupSession;

    @GetMapping("/profile")
    public String profile(@NonNull Authentication authentication, @NonNull Model model) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        final UserProfileDbo profile = userProfileService.getProfile(userId);
        if (!model.containsAttribute(PROFILE_FORM_ATTRIBUTE)) {
            model.addAttribute(PROFILE_FORM_ATTRIBUTE, profileForm(profile));
        }
        model.addAttribute(PROFILE_EMAIL_ATTRIBUTE, profile.email());
        return PROFILE_VIEW;
    }

    @PostMapping("/profile")
    public String updateProfile(@NonNull Authentication authentication,
                                @Valid @ModelAttribute(PROFILE_FORM_ATTRIBUTE) UserProfileUpdate profileForm,
                                BindingResult bindingResult,
                                @NonNull Model model,
                                @NonNull RedirectAttributes redirectAttributes,
                                @NonNull HttpServletRequest request) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);

        if (bindingResult.hasErrors()) {
            final UserProfileDbo profile = userProfileService.getProfile(userId);
            model.addAttribute(PROFILE_EMAIL_ATTRIBUTE, profile.email());
            return PROFILE_VIEW;
        }

        userProfileService.updateProfile(userId, profileForm);
        if (profileSetupSession.isProfileSetupRequired(request)) {
            profileSetupSession.completeProfileSetup(request);
            return REMEMBER_LOGIN_REDIRECT;
        }
        redirectAttributes.addFlashAttribute(PROFILE_SAVED_ATTRIBUTE, true);
        return PROFILE_REDIRECT;
    }

    private UserProfileUpdate profileForm(UserProfileDbo profile) {
        return new UserProfileUpdate(profile.displayName(), profile.pictureUrl());
    }
}
