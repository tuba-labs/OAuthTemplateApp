package org.tubalabs.app.users.profile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class DisplayNameValidator implements ConstraintValidator<ValidDisplayName, String> {

    private static final int MINIMUM_ALPHANUMERIC_CHARACTERS = 3;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        int alphanumericCharacters = 0;
        for (int index = 0; index < value.length(); index++) {
            if (Character.isLetterOrDigit(value.charAt(index))) {
                alphanumericCharacters++;
            }
        }
        return alphanumericCharacters >= MINIMUM_ALPHANUMERIC_CHARACTERS;
    }
}
