package org.tubalabs.app.users.identity.externalidentity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ExternalIdentityLinkSession {

    private static final String PENDING_LINK_ATTRIBUTE = "externalIdentityLink.pending";
    private static final String SUCCESS_ATTRIBUTE = "externalIdentityLink.success";
    private static final String FAILURE_ATTRIBUTE = "externalIdentityLink.failure";

    public void start(@NonNull HttpServletRequest request,
                      @NonNull UUID userId,
                      @NonNull String providerId,
                      @NonNull Authentication originalAuthentication) {
        request.getSession().setAttribute(PENDING_LINK_ATTRIBUTE, new PendingExternalIdentityLink(
                userId, providerId, originalAuthentication));
    }

    public Optional<PendingExternalIdentityLink> pending(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        final Object value = session.getAttribute(PENDING_LINK_ATTRIBUTE);
        if (value instanceof PendingExternalIdentityLink pendingExternalIdentityLink) {
            return Optional.of(pendingExternalIdentityLink);
        }
        return Optional.empty();
    }

    public void complete(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession();
        session.removeAttribute(PENDING_LINK_ATTRIBUTE);
        session.setAttribute(SUCCESS_ATTRIBUTE, Boolean.TRUE);
    }

    public void fail(@NonNull HttpServletRequest request, @NonNull IdentityLinkFailure reason) {
        final HttpSession session = request.getSession();
        session.removeAttribute(PENDING_LINK_ATTRIBUTE);
        session.setAttribute(FAILURE_ATTRIBUTE, reason);
    }

    public boolean consumeSuccess(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        final Object value = session.getAttribute(SUCCESS_ATTRIBUTE);
        session.removeAttribute(SUCCESS_ATTRIBUTE);
        return Boolean.TRUE.equals(value);
    }

    public Optional<IdentityLinkFailure> consumeFailure(@NonNull HttpServletRequest request) {
        return consumeFailure(request, FAILURE_ATTRIBUTE);
    }

    private Optional<IdentityLinkFailure> consumeFailure(@NonNull HttpServletRequest request,
                                                        @NonNull String attributeName) {
        final HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        final Object value = session.getAttribute(attributeName);
        session.removeAttribute(attributeName);
        if (value instanceof IdentityLinkFailure failure) {
            return Optional.of(failure);
        }
        return Optional.empty();
    }

    public record PendingExternalIdentityLink(
            @NonNull UUID userId,
            @NonNull String providerId,
            @NonNull Authentication originalAuthentication) {
    }
}
