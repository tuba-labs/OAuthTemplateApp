package org.tubalabs.app.events.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.tubalabs.app.events.EventLogService;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.events.UserIdentityEventFactory;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogoutEventHandler implements LogoutHandler {

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver;
    private final EventLogService eventLogService;
    private final UserIdentityEventFactory userIdentityEventFactory;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        final Optional<UserIdentityDbo> identity = currentLoginIdentityResolver.identity(authentication);
        final String clientIp = clientIp(request);
        final String userAgent = userAgent(request);
        try {
            eventLogService.record(identity
                    .map(value -> userIdentityEventFactory.logout(value, clientIp, userAgent))
                    .orElseGet(() -> userIdentityEventFactory.unresolvedLogout(
                            authentication.getName(), clientIp, userAgent)));
        } catch (RuntimeException exception) {
            log.warn("Could not record logout event: {}", exception.toString());
        }
    }

    private String clientIp(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(@NonNull HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }
}
