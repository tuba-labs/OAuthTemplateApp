package org.tubalabs.app.security.remember;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RememberLoginPromptService {

    private static final String REMEMBER_LOGIN_PROMPT_AFTER_COOKIE = "remember_login_prompt_after";

    private final Clock clock;
    private final RememberLoginProperties rememberLoginProperties;

    public boolean shouldAsk(@NonNull HttpServletRequest request) {
        final Cookie cookie = WebUtils.getCookie(request, REMEMBER_LOGIN_PROMPT_AFTER_COOKIE);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return true;
        }
        try {
            final Instant askAfter = Instant.ofEpochMilli(Long.parseLong(cookie.getValue()));
            return !askAfter.isAfter(clock.instant());
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    public void rememberSkip(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        final Instant askAfter = clock.instant().plus(rememberLoginProperties.promptSkipDuration());
        addCookie(request, response, Long.toString(askAfter.toEpochMilli()), rememberLoginProperties.promptSkipDuration());
    }

    public void clearSkip(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        addCookie(request, response, "", Duration.ZERO);
    }

    private void addCookie(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull String value,
                           @NonNull Duration maxAge) {
        final ResponseCookie cookie = ResponseCookie.from(REMEMBER_LOGIN_PROMPT_AFTER_COOKIE, value)
                .path("/")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
