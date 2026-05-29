package org.tubalabs.app.users.identity;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.tubalabs.app.security.remember.RememberedLoginName;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.externalidentity.ExternalIdentity;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProvider;
import org.tubalabs.app.users.identity.externalidentity.providers.ExternalIdentityProviders;
import org.tubalabs.app.users.identity.password.LocalEmailNormalizer;
import org.tubalabs.app.users.identity.password.LocalUserService;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentLoginIdentityResolver {

    private final LocalEmailNormalizer emailNormalizer;
    private final UserIdentityRepository userIdentityRepository;
    private final ExternalIdentityProviders externalIdentityProviders;

    public Optional<UserIdentityDbo> identity(@NonNull Authentication authentication) {
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication instanceof OAuth2AuthenticationToken authenticationToken) {
            return oauthIdentity(authenticationToken);
        }

        final Optional<UserIdentityDbo> rememberedIdentity = rememberedIdentity(authentication.getName());
        if (rememberedIdentity.isPresent()) {
            return rememberedIdentity;
        }

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return localIdentity(authentication.getName());
        }
        return Optional.empty();
    }

    private Optional<UserIdentityDbo> oauthIdentity(OAuth2AuthenticationToken authentication) {
        final String providerId = authentication.getAuthorizedClientRegistrationId();
        final OAuth2User oauth2User = authentication.getPrincipal();
        final ExternalIdentityProvider provider = externalIdentityProviders.getProvider(providerId);
        final ExternalIdentity externalIdentity = provider.getIdentity(oauth2User);
        return userIdentityRepository.findByProviderAndSubject(providerId, externalIdentity.subject());
    }

    private Optional<UserIdentityDbo> rememberedIdentity(String username) {
        try {
            final UUID identityId = RememberedLoginName.identityId(username);
            return userIdentityRepository.findById(identityId);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<UserIdentityDbo> localIdentity(String username) {
        final String normalizedEmail = emailNormalizer.normalize(username);
        return userIdentityRepository.findByProviderAndSubject(LocalUserService.LOCAL_PROVIDER_ID, normalizedEmail);
    }
}
