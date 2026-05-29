package org.tubalabs.app.security.remember;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;
import org.tubalabs.app.users.identity.db.UserIdentityRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class RememberedUserDetailsServiceTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID IDENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String INVALID_IDENTITY_ID = "not-an-identity-id";
    private static final String PROVIDER_ID = "google";
    private static final String SUBJECT = "google-subject";

    private final UserIdentityRepository userIdentityRepository = Mockito.mock(UserIdentityRepository.class);
    private final RememberedUserDetailsService service = new RememberedUserDetailsService(userIdentityRepository);

    @Test
    void loadsRememberedUserByIdentityId() {
        final String username = RememberedLoginName.username(IDENTITY_ID);
        when(userIdentityRepository.findById(IDENTITY_ID)).thenReturn(Optional.of(identity()));

        final UserDetails userDetails = service.loadUserByUsername(username);

        assertThat(userDetails.getUsername()).isEqualTo(IDENTITY_ID.toString());
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void rejectsInvalidIdentityId() {
        assertThatThrownBy(() -> service.loadUserByUsername(INVALID_IDENTITY_ID))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void rejectsUnknownIdentityId() {
        when(userIdentityRepository.findById(IDENTITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(IDENTITY_ID.toString()))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private UserIdentityDbo identity() {
        return UserIdentityDbo.builder()
                .id(IDENTITY_ID)
                .userId(USER_ID)
                .providerId(PROVIDER_ID)
                .subject(SUBJECT)
                .build();
    }
}
