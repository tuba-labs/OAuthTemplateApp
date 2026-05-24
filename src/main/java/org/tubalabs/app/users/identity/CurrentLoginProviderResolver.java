package org.tubalabs.app.users.identity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentLoginProviderResolver {

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver;

    public Optional<String> providerId(@NonNull Authentication authentication, @NonNull HttpServletRequest request) {
        return providerId(authentication);
    }

    public Optional<String> providerId(@NonNull Authentication authentication) {
        return currentLoginIdentityResolver.identity(authentication)
                .map(UserIdentityDbo::providerId);
    }
}
