package org.tubalabs.app.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.user.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RememberedUserDetailsService {

    private final UserRepository userRepository;

    public UserDetails loadUserByUsername(@NonNull String username) {
        final UUID userId = parseUserId(username);
        userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Remembered user not found: " + userId));

        return User.withUsername(userId.toString())
                .password("{noop}remembered")
                .authorities("ROLE_USER")
                .build();
    }

    private UUID parseUserId(String username) {
        try {
            return UUID.fromString(username);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("Invalid remembered user id: " + username, exception);
        }
    }
}
