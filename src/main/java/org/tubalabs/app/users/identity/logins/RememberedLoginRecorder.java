package org.tubalabs.app.users.identity.logins;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RememberedLoginRecorder {

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver;
    private final UserIdentityAuditLog userIdentityAuditLog;

    @EventListener
    public void recordRememberedLogin(@NonNull InteractiveAuthenticationSuccessEvent event) {
        if (!isRememberMeAutoLogin(event)) {
            return;
        }

        final Authentication authentication = event.getAuthentication();
        final Optional<UserIdentityDbo> identity = currentLoginIdentityResolver.identity(authentication);
        if (identity.isEmpty()) {
            log.warn("Could not resolve remembered login identity for principal: {}", authentication.getName());
            return;
        }

        final UserIdentityDbo resolvedIdentity = identity.orElseThrow();
        final Optional<HttpServletRequest> request = currentRequest();
        final String clientIp = clientIp(request, authentication);
        final String userAgent = userAgent(request);

        userIdentityAuditLog.recordLogin(resolvedIdentity, clientIp, userAgent, true, false);
    }

    private boolean isRememberMeAutoLogin(InteractiveAuthenticationSuccessEvent event) {
        return RememberMeAuthenticationFilter.class.equals(event.getGeneratedBy())
                && event.getAuthentication() instanceof RememberMeAuthenticationToken;
    }

    private Optional<HttpServletRequest> currentRequest() {
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return Optional.of(servletRequestAttributes.getRequest());
        }
        return Optional.empty();
    }

    private String clientIp(Optional<HttpServletRequest> request, Authentication authentication) {
        return request
                .map(HttpServletRequest::getRemoteAddr)
                .or(() -> remoteAddress(authentication))
                .orElse("");
    }

    private Optional<String> remoteAddress(Authentication authentication) {
        if (authentication.getDetails() instanceof WebAuthenticationDetails details) {
            return Optional.ofNullable(details.getRemoteAddress());
        }
        return Optional.empty();
    }

    private String userAgent(Optional<HttpServletRequest> request) {
        return request
                .map(value -> value.getHeader("User-Agent"))
                .orElse("");
    }
}
