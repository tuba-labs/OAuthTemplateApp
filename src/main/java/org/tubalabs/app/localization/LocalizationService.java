package org.tubalabs.app.localization;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalizationService {

    private final @NonNull MessageSource messageSource;

    public String message(@NonNull String localizationKey, Object... arguments) {
        return messageSource.getMessage(localizationKey, arguments, LocaleContextHolder.getLocale());
    }

    public String message(@NonNull LocalizationKey localizationKey, Object... arguments) {
        return message(localizationKey.getLocalizationKey(), arguments);
    }
}
