package org.tubalabs.app.users.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class UserLanguageLocaleResolverTest {

    private static final Locale NORWEGIAN_LOCALE = UserLanguage.NORWEGIAN.locale();

    private final UserLanguageLocaleResolver localeResolver = new UserLanguageLocaleResolver();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void resolvesDefaultLanguageWhenRequestHasNoSelection() {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(localeResolver.resolveLocale(request)).isEqualTo(UserLanguage.DEFAULT.locale());
    }

    @Test
    void exposesSelectedLanguageToSpringMvcRequestContext() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, localeResolver);

        UserLanguageLocaleResolver.setRequestLocale(request, NORWEGIAN_LOCALE);

        assertThat(localeResolver.resolveLocale(request)).isEqualTo(NORWEGIAN_LOCALE);
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(NORWEGIAN_LOCALE);
        assertThat(RequestContextUtils.getLocale(request)).isEqualTo(NORWEGIAN_LOCALE);
    }
}
