package org.tubalabs.app.users.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.current.CurrentUserSession;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UserLanguageLocaleInterceptorTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String LANGUAGE_PARAMETER = "language";

    private final CurrentUserSession currentUserSession = Mockito.mock(CurrentUserSession.class);
    private final UserLanguageLocaleInterceptor interceptor = new UserLanguageLocaleInterceptor(currentUserSession);
    private final UserLanguageLocaleResolver localeResolver = new UserLanguageLocaleResolver();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void usesCurrentUserLanguageWhenAuthenticated() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        when(currentUserSession.currentUser(request)).thenReturn(Optional.of(currentUser(UserLanguage.NORWEGIAN)));

        final boolean continueRequest = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(continueRequest).isTrue();
        assertThat(localeResolver.resolveLocale(request)).isEqualTo(UserLanguage.NORWEGIAN.locale());
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(UserLanguage.NORWEGIAN.locale());
    }

    @Test
    void usesRequestLanguageWhenAnonymous() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(LANGUAGE_PARAMETER, UserLanguage.NORWEGIAN.tag());
        when(currentUserSession.currentUser(request)).thenReturn(Optional.empty());

        final boolean continueRequest = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(continueRequest).isTrue();
        assertThat(localeResolver.resolveLocale(request)).isEqualTo(UserLanguage.NORWEGIAN.locale());
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(UserLanguage.NORWEGIAN.locale());
    }

    private CurrentUser currentUser(UserLanguage language) {
        return new CurrentUser(USER_ID, "Person", null, false, null, true, language.tag());
    }
}
