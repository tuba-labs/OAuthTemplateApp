package org.tubalabs.app.users.password;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalEmailNormalizerTest {

    private static final String MIXED_CASE_EMAIL_WITH_SPACES = "  Person@Example.COM  ";
    private static final String NORMALIZED_EMAIL = "person@example.com";
    private static final String BLANK_EMAIL = "   ";

    private final LocalEmailNormalizer normalizer = new LocalEmailNormalizer();

    @Test
    void trimsAndLowercasesEmail() {
        assertThat(normalizer.normalize(MIXED_CASE_EMAIL_WITH_SPACES)).isEqualTo(NORMALIZED_EMAIL);
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> normalizer.normalize(BLANK_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
