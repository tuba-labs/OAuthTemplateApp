package org.tubalabs.app.users.identity;

import lombok.NonNull;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserLoginTypeService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserPasswordCredentialRepository userPasswordCredentialRepository;

    public UserLoginTypeService(@NonNull ClientRegistrationRepository clientRegistrationRepository,
                                @NonNull UserIdentityRepository userIdentityRepository,
                                @NonNull UserPasswordCredentialRepository userPasswordCredentialRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.userPasswordCredentialRepository = userPasswordCredentialRepository;
    }

    public List<String> availableExternalLoginTypes(@NonNull UUID userId) {
        final Set<String> linkedProviderIds = userIdentityRepository.findByUserId(userId)
                .stream()
                .map(UserIdentityDbo::providerId)
                .collect(Collectors.toSet());

        final List<String> providerIds = new ArrayList<>();
        if (!(clientRegistrationRepository instanceof Iterable<?> clientRegistrations)) {
            return List.of();
        }

        for (Object item : clientRegistrations) {
            if (item instanceof ClientRegistration clientRegistration
                    && !linkedProviderIds.contains(clientRegistration.getRegistrationId())) {
                providerIds.add(clientRegistration.getRegistrationId());
            }
        }
        return List.copyOf(providerIds);
    }

    public boolean canLinkExternalLoginType(@NonNull UUID userId, @NonNull String providerId) {
        return availableExternalLoginTypes(userId)
                .stream()
                .anyMatch(availableProviderId -> availableProviderId.equals(providerId));
    }

    public boolean canLinkLocalLoginType(@NonNull UUID userId) {
        return userIdentityRepository.findByUserIdAndProviderId(userId, LocalUserService.LOCAL_PROVIDER_ID)
                .isEmpty();
    }

    public List<LinkedLoginType> linkedLoginTypes(@NonNull UUID userId,
                                                  @NonNull Optional<String> currentProviderId) {
        final List<UserIdentityDbo> linkedIdentities = userIdentityRepository.findByUserId(userId);
        return linkedIdentities.stream()
                .map(identity -> linkedLoginType(identity.providerId(), currentProviderId, linkedIdentities.size()))
                .toList();
    }

    @Transactional
    public void unlinkLoginType(@NonNull UUID userId,
                                @NonNull String providerId,
                                @NonNull Optional<String> currentProviderId) {
        if (currentProviderId.isEmpty()) {
            throw new LoginTypeUnlinkException(LoginTypeUnlinkFailure.UNKNOWN_CURRENT_LOGIN);
        }
        if (currentProviderId.filter(providerId::equals).isPresent()) {
            throw new LoginTypeUnlinkException(LoginTypeUnlinkFailure.CURRENT_LOGIN);
        }
        final List<UserIdentityDbo> linkedIdentities = userIdentityRepository.findByUserId(userId);
        final boolean linked = linkedIdentities.stream()
                .anyMatch(identity -> identity.providerId().equals(providerId));
        if (!linked) {
            //already deleted
           return;
        }
        if (linkedIdentities.size() <= 1) {
            throw new LoginTypeUnlinkException(LoginTypeUnlinkFailure.LAST_LOGIN_TYPE);
        }
        if (LocalUserService.LOCAL_PROVIDER_ID.equals(providerId)) {
            userPasswordCredentialRepository.deleteByUserId(userId);
        }
        userIdentityRepository.deleteByUserIdAndProviderId(userId, providerId);
    }

    private LinkedLoginType linkedLoginType(String providerId,
                                           Optional<String> currentProviderId,
                                           int linkedIdentityCount) {
        final boolean current = currentProviderId.filter(providerId::equals).isPresent();
        return new LinkedLoginType(providerId, current, unlinkState(current, currentProviderId, linkedIdentityCount));
    }

    private LoginTypeUnlinkState unlinkState(boolean current,
                                             Optional<String> currentProviderId,
                                             int linkedIdentityCount) {
        if (current) {
            return LoginTypeUnlinkState.CURRENT_LOGIN;
        }
        if (linkedIdentityCount <= 1) {
            return LoginTypeUnlinkState.LAST_LOGIN_TYPE;
        }
        if (currentProviderId.isEmpty()) {
            return LoginTypeUnlinkState.UNKNOWN_CURRENT_LOGIN;
        }
        return LoginTypeUnlinkState.AVAILABLE;
    }
}
