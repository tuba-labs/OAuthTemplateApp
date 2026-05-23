package org.tubalabs.app.users.profile.api.ui;

import jakarta.validation.constraints.NotBlank;
import org.tubalabs.app.users.password.validation.SafePassword;

public record UserPasswordChange(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @SafePassword
        String newPassword,
        @NotBlank(message = "Password confirmation is required")
        String newPasswordConfirmation) {
}
