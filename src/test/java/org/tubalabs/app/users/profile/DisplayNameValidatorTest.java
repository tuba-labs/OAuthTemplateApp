package org.tubalabs.app.users.profile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayNameValidatorTest {

    private final DisplayNameValidator validator = new DisplayNameValidator();

    @Test
    void acceptsDisplayNameWithAtLeastThreeAlphanumericCharacters() {
        assertThat(validator.isValid("A B 3", null)).isTrue();
    }

    @Test
    void rejectsDisplayNameWithFewerThanThreeAlphanumericCharacters() {
        assertThat(validator.isValid("A . 2", null)).isFalse();
    }

    @Test
    void allowsBlankValueForRequiredValidatorToHandle() {
        assertThat(validator.isValid("", null)).isTrue();
    }
}
