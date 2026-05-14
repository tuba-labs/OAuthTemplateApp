package org.tubalabs.app.security.identity;

import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExternalIdentityProviders {

    private final Map<String, ExternalIdentityProvider>
            providersById;

    public ExternalIdentityProviders(@NonNull List<ExternalIdentityProvider> providers) {

        this.providersById = providers.stream().collect(
                Collectors.toUnmodifiableMap(
                        ExternalIdentityProvider::providerId, Function.identity()));
    }

    @NonNull
    public ExternalIdentityProvider getProvider(@NonNull String providerId) {

        final ExternalIdentityProvider provider =
                providersById.get(providerId);

        if (provider == null) {
            throw new IllegalStateException(
                    "Unsupported provider: "
                            + providerId);
        }

        return provider;
    }
}