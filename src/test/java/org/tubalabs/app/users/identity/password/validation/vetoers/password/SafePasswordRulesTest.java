package org.tubalabs.app.users.identity.password.validation.vetoers.password;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.tubalabs.app.users.identity.password.validation.vetoers.password.SafePasswordRules;

import static org.assertj.core.api.Assertions.assertThat;

class SafePasswordRulesTest {

    private static final String NULL_PASSWORD = null;

    @ParameterizedTest
    @CsvSource({
            "ValidPassword1,true",
            "Valid password!,true",
            "short1,false",
            "LongEnoughButNoNumberOrSymbol,false",
            "123456789012,false",
            "password with spaces,false"
    })
    void validatesPasswordRules(String password, boolean expectedSafe) {
        assertThat(SafePasswordRules.isSafe(password)).isEqualTo(expectedSafe);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankPasswordsAreLeftToRequiredFieldValidation(String password) {
        assertThat(SafePasswordRules.isSafe(password)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "129,false",
            "128,true"
    })
    void enforcesMaximumLength(int length, boolean expectedSafe) {
        final String password = "A".repeat(length - 1) + "1";

        assertThat(SafePasswordRules.isSafe(password)).isEqualTo(expectedSafe);
    }

    @Test
    void nullPasswordIsLeftToRequiredFieldValidation() {
        assertThat(SafePasswordRules.isSafe(NULL_PASSWORD)).isTrue();
    }
}
