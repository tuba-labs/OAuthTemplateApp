package org.tubalabs.app.ui.language;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.ui.language.dtos.LanguageOptionDto;
import org.tubalabs.app.users.settings.UserLanguage;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LanguageSelectorPageModel {

    public static final String LANGUAGE_SELECTOR_OPTIONS_ATTRIBUTE = "languageSelectorOptions";
    public static final String LANGUAGE_SELECTOR_CURRENT_TAG_ATTRIBUTE = "languageSelectorCurrentTag";
    public static final String LANGUAGE_SELECTOR_PERSISTENCE_ATTRIBUTE = "languageSelectorPersistence";

    private final @NonNull LocalizationService localizationService;

    public void addLanguageSelector(@NonNull Model model,
                                    @NonNull UserLanguage currentLanguage,
                                    @NonNull LanguageSelectorPersistence persistence) {
        model.addAttribute(LANGUAGE_SELECTOR_OPTIONS_ATTRIBUTE, languageOptions());
        model.addAttribute(LANGUAGE_SELECTOR_CURRENT_TAG_ATTRIBUTE, currentLanguage.tag());
        model.addAttribute(LANGUAGE_SELECTOR_PERSISTENCE_ATTRIBUTE, persistence.attributeValue());
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
