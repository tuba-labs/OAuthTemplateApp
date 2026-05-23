package org.tubalabs.app.users.password;

import org.junit.jupiter.api.Test;
import org.tubalabs.app.users.password.validation.vetoers.CreationVetoResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateResultTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String VETO_ID = "pwned-password";
    private static final String VETO_REASON = "Password has appeared in a known breach";

    @Test
    void createdResultContainsUserIdAndNoVetoes() {
        final CreateResult result = CreateResult.created(USER_ID);

        assertThat(result.id()).isEqualTo(USER_ID);
        assertThat(result.creationVetoed()).isEmpty();
        assertThat(result.vetoed()).isFalse();
    }

    @Test
    void vetoedResultContainsVetoAndNoUserId() {
        final CreationVetoResult veto = CreationVetoResult.stop(VETO_ID, VETO_REASON);
        final CreateResult result = CreateResult.vetoed(List.of(veto));

        assertThat(result.id()).isNull();
        assertThat(result.creationVetoed()).containsExactly(veto);
        assertThat(result.vetoed()).isTrue();
        assertThat(result.firstVeto()).isEqualTo(veto);
    }

    @Test
    void rejectsResultWithBothUserIdAndVetoes() {
        final CreationVetoResult veto = CreationVetoResult.stop(VETO_ID, VETO_REASON);

        assertThatThrownBy(() -> new CreateResult(USER_ID, List.of(veto)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsResultWithoutUserIdOrVetoes() {
        assertThatThrownBy(() -> new CreateResult(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
