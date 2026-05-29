package org.tubalabs.app.security.remember;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RememberedUserDetailsService {

    private final UserIdentityRepository userIdentityRepository;

    public UserDetails loadUserByUsername(@NonNull String username) {
        final UUID identityId = parseIdentityId(username);
        userIdentityRepository.findById(identityId)
                .orElseThrow(() -> new UsernameNotFoundException("Remembered identity not found: " + identityId));

        return User.withUsername(username)
                .password("{noop}remembered")
                .authorities("ROLE_USER")
                .build();
    }

    private UUID parseIdentityId(String username) {
        try {
            return RememberedLoginName.identityId(username);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("Invalid remembered identity id: " + username, exception);
        }
    }
}
