package org.tubalabs.app.users.password.validation.vetoers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreationVetoResultTest {

    private static final String VETO_ID = "pwned-password";
    private static final String VETO_REASON = "Password has appeared in a known breach";

    @Test
    void okResultHasNoReason() {
        final CreationVetoResult result = CreationVetoResult.ok();

        assertThat(result.accepted()).isTrue();
        assertThat(result.id()).isNull();
        assertThat(result.englishReason()).isNull();
    }

    @Test
    void stopResultRequiresIdAndReason() {
        final CreationVetoResult result = CreationVetoResult.stop(VETO_ID, VETO_REASON);

        assertThat(result.accepted()).isFalse();
        assertThat(result.id()).isEqualTo(VETO_ID);
        assertThat(result.englishReason()).isEqualTo(VETO_REASON);
    }

    @Test
    void rejectsNegativeResultWithoutReason() {
        assertThatThrownBy(() -> new CreationVetoResult(false, VETO_ID, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPositiveResultWithReason() {
        assertThatThrownBy(() -> new CreationVetoResult(true, VETO_ID, VETO_REASON))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
