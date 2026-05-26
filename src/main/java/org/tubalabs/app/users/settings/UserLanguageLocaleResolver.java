package org.tubalabs.app.users.settings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

public class UserLanguageLocaleResolver implements LocaleResolver {

    private static final String LOCALE_ATTRIBUTE = UserLanguageLocaleResolver.class.getName() + ".locale";

    @Override
    public Locale resolveLocale(@NonNull HttpServletRequest request) {
        final Object locale = request.getAttribute(LOCALE_ATTRIBUTE);
        if (locale instanceof Locale requestLocale) {
            return requestLocale;
        }
        return UserLanguage.DEFAULT.locale();
    }

    @Override
    public void setLocale(@NonNull HttpServletRequest request,
                          HttpServletResponse response,
                          Locale locale) {
        if (locale == null) {
            request.removeAttribute(LOCALE_ATTRIBUTE);
            LocaleContextHolder.resetLocaleContext();
            return;
        }
        setRequestLocale(request, locale);
    }

    public static void setRequestLocale(@NonNull HttpServletRequest request, @NonNull Locale locale) {
        request.setAttribute(LOCALE_ATTRIBUTE, locale);
        LocaleContextHolder.setLocale(locale);
    }
}
