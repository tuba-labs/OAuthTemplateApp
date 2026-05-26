package org.tubalabs.app.ui.profile.changepassword.dtos;

import jakarta.validation.constraints.NotBlank;
import org.tubalabs.app.users.identity.password.validation.SafePassword;

public record UserPasswordChangeDto(
        @NotBlank(message = "{validation.password.current.required}")
        String currentPassword,
        @NotBlank(message = "{validation.password.new.required}")
        @SafePassword
        String newPassword,
        @NotBlank(message = "{validation.password-confirmation.required}")
        String newPasswordConfirmation) {
}
