package org.tubalabs.app.users;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.externalidentity.providers.ExternalIdentityProviders;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.password.LocalEmailNormalizer;
import org.tubalabs.app.users.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.password.db.UserPasswordCredentialRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserIdResolver {

    private final LocalEmailNormalizer emailNormalizer;
    private final UserPasswordCredentialRepository userPasswordCredentialRepository;
    private final ExternalIdentityProviders externalIdentityProviders;
    private final UserIdentityRepository userIdentityRepository;

    public UUID requireUserId(@NonNull Authentication authentication) {
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        if (authentication instanceof OAuth2AuthenticationToken authenticationToken) {
            return oauth2UserId(authenticationToken);
        }
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return localUserId(authentication);
        }

        throw new AccessDeniedException("Unsupported authentication type: " + authentication.getClass().getName());
    }

    private UUID localUserId(Authentication authentication) {
        final String normalizedEmail = emailNormalizer.normalize(authentication.getName());
        return userPasswordCredentialRepository.findByEmail(normalizedEmail)
                .map(UserPasswordCredentialDbo::userId)
                .orElseThrow(() -> new AccessDeniedException("Local user not found: " + normalizedEmail));
    }

    private UUID oauth2UserId(OAuth2AuthenticationToken authentication) {
        final String providerId = authentication.getAuthorizedClientRegistrationId();
        final OAuth2User oauth2User = authentication.getPrincipal();
        final ExternalIdentityProvider provider = externalIdentityProviders.getProvider(providerId);
        final ExternalIdentity externalIdentity = provider.getIdentity(oauth2User);
        return userIdentityRepository.findByProviderAndSubject(providerId, externalIdentity.subject())
                .map(UserIdentityDbo::userId)
                .orElseThrow(() -> new AccessDeniedException("OAuth user not found: " + providerId));
    }
}
