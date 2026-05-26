package org.tubalabs.app.ui.profile.profile.menusystem;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.ui.language.dtos.LanguageOptionDto;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.profile.db.UserProfileDbo;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfilePageModel {

    public static final String LOCAL_PROFILE_ATTRIBUTE = "localProfile";
    public static final String LOCAL_LOGIN_NAME_ATTRIBUTE = "localLoginName";
    public static final String PROFILE_PICTURE_ATTRIBUTE = "profilePictureUrl";
    public static final String LANGUAGE_OPTIONS_ATTRIBUTE = "languageOptions";

    private final @NonNull LocalizationService localizationService;

    public void addProfileAttributes(@NonNull Model model,
                              @NonNull CurrentUser currentUser,
                              @NonNull UserProfileDbo profile) {
        model.addAttribute(LOCAL_PROFILE_ATTRIBUTE, currentUser.localProfile());
        if (currentUser.localProfile()) {
            model.addAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE, currentUser.localLoginName());
        }
        model.addAttribute(PROFILE_PICTURE_ATTRIBUTE, profile.pictureUrl());
        model.addAttribute(LANGUAGE_OPTIONS_ATTRIBUTE, languageOptions());
    }

    private List<LanguageOptionDto> languageOptions() {
        return UserLanguage.supportedLanguages()
                .stream()
                .map(language -> new LanguageOptionDto(
                        language.tag(),
                        language.flag(),
                        localizationService.message(language)))
                .toList();
    }
}
