package org.tubalabs.app.users;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserIdResolver {

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver;

    public UUID requireUserId(@NonNull Authentication authentication) {
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return currentLoginIdentityResolver.identity(authentication)
                .map(UserIdentityDbo::userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Current login identity not found for authentication type: " + authentication.getClass().getName()));
    }
}
