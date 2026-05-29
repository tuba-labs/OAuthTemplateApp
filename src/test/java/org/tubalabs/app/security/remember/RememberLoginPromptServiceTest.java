package org.tubalabs.app.security.remember;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RememberLoginPromptServiceTest {

    private static final String REMEMBER_LOGIN_PROMPT_AFTER_COOKIE = "remember_login_prompt_after";
    private static final Instant NOW = Instant.parse("2026-05-23T12:00:00Z");
    private static final Duration PROMPT_SKIP_DURATION = Duration.ofDays(30);
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(183);
    private static final String REMEMBER_ME_KEY = "test-remember-me-key";
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final RememberLoginPromptService rememberLoginPromptService = new RememberLoginPromptService(
            CLOCK,
            new RememberLoginProperties(PROMPT_SKIP_DURATION, TOKEN_VALIDITY, REMEMBER_ME_KEY));

    @Test
    void asksWhenPromptSkipCookieIsMissing() {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(rememberLoginPromptService.shouldAsk(request)).isTrue();
    }

    @Test
    void doesNotAskWhenPromptSkipIsStillActive() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(promptAfterCookie(NOW.plus(Duration.ofDays(1))));

        assertThat(rememberLoginPromptService.shouldAsk(request)).isFalse();
    }

    @Test
    void asksWhenPromptSkipIsDue() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(promptAfterCookie(NOW.minus(Duration.ofSeconds(1))));

        assertThat(rememberLoginPromptService.shouldAsk(request)).isTrue();
    }

    @Test
    void asksWhenPromptSkipCookieIsInvalid() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(REMEMBER_LOGIN_PROMPT_AFTER_COOKIE, "not-a-date"));

        assertThat(rememberLoginPromptService.shouldAsk(request)).isTrue();
    }

    @Test
    void rememberingSkipStoresNextPromptTimeInBrowserCookie() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        rememberLoginPromptService.rememberSkip(request, response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains(
                        REMEMBER_LOGIN_PROMPT_AFTER_COOKIE + "="
                                + NOW.plus(PROMPT_SKIP_DURATION).toEpochMilli(),
                        "Max-Age=" + PROMPT_SKIP_DURATION.toSeconds(),
                        "Path=/",
                        "HttpOnly",
                        "SameSite=Lax");
    }

    @Test
    void clearingSkipExpiresPromptCookie() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        rememberLoginPromptService.clearSkip(request, response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains(
                        REMEMBER_LOGIN_PROMPT_AFTER_COOKIE + "=",
                        "Max-Age=0",
                        "Path=/",
                        "HttpOnly",
                        "SameSite=Lax");
    }

    private Cookie promptAfterCookie(Instant askAfter) {
        return new Cookie(REMEMBER_LOGIN_PROMPT_AFTER_COOKIE, Long.toString(askAfter.toEpochMilli()));
    }
}
