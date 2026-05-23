package org.tubalabs.app.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.tubalabs.app.users.user.UserDbo;
import org.tubalabs.app.users.user.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class RememberedUserDetailsServiceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String INVALID_USER_ID = "not-a-user-id";

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final RememberedUserDetailsService service = new RememberedUserDetailsService(userRepository);

    @Test
    void loadsRememberedUserByInternalUserId() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(new UserDbo(USER_ID)));

        final UserDetails userDetails = service.loadUserByUsername(USER_ID.toString());

        assertThat(userDetails.getUsername()).isEqualTo(USER_ID.toString());
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void rejectsInvalidUserId() {
        assertThatThrownBy(() -> service.loadUserByUsername(INVALID_USER_ID))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsUnknownUserId() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(USER_ID.toString()))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
