package org.tubalabs.app.ui.profile.changepassword.dtos;

import jakarta.validation.constraints.NotBlank;
import org.tubalabs.app.users.identity.password.validation.SafePassword;

public record UserPasswordChangeDto(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @SafePassword
        String newPassword,
        @NotBlank(message = "Password confirmation is required")
        String newPasswordConfirmation) {
}
