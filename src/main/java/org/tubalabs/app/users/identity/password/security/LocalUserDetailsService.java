package org.tubalabs.app.users.identity.password.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.identity.password.LocalEmailNormalizer;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialDbo;
import org.tubalabs.app.users.identity.password.db.UserPasswordCredentialRepository;

@Service
@RequiredArgsConstructor
public class LocalUserDetailsService implements UserDetailsService {

    private final LocalEmailNormalizer emailNormalizer;
    private final UserPasswordCredentialRepository userPasswordCredentialRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) {
        final String normalizedEmail = emailNormalizer.normalize(email);
        final UserPasswordCredentialDbo credential = userPasswordCredentialRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown local user: " + normalizedEmail));

        return User.withUsername(credential.email())
                .password(credential.passwordHash())
                .authorities("ROLE_USER")
                .build();
    }
}
