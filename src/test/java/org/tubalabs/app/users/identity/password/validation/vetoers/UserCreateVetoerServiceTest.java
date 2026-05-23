package org.tubalabs.app.users.identity.password.validation.vetoers;

import org.junit.jupiter.api.Test;
import org.tubalabs.app.users.identity.password.LocalUserRegistration;
import org.tubalabs.app.users.identity.password.validation.vetoers.CreationVetoResult;
import org.tubalabs.app.users.identity.password.validation.vetoers.UserCreateVetoerService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserCreateVetoerServiceTest {

    private static final String EMAIL = "person@example.com";
    private static final String PASSWORD = "ValidPassword1";
    private static final String VETO_ID = "pwned-password";
    private static final String VETO_REASON = "Password has appeared in a known breach";

    private static final LocalUserRegistration REGISTRATION =
            new LocalUserRegistration(EMAIL, PASSWORD);

    @Test
    void returnsOnlyNegativeValidatorResults() {
        final CreationVetoResult veto = CreationVetoResult.stop(VETO_ID, VETO_REASON);
        final UserCreateVetoerService service = new UserCreateVetoerService(List.of(
                registration -> CreationVetoResult.ok(),
                registration -> veto));

        final List<CreationVetoResult> vetoes = service.getVetoes(REGISTRATION);

        assertThat(vetoes).containsExactly(veto);
    }
}
