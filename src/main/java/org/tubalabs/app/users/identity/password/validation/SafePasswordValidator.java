package org.tubalabs.app.users.identity.password.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.tubalabs.app.users.identity.password.validation.vetoers.password.SafePasswordRules;

public class SafePasswordValidator implements ConstraintValidator<SafePassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return SafePasswordRules.isSafe(password);
    }
}
