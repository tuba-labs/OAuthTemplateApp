package org.tubalabs.app.users.identity.password.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.tubalabs.app.users.identity.password.validation.SafePassword;

public record LocalRegistrationRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must be 320 characters or fewer")
        String email,
        @NotBlank(message = "Password is required")
        @SafePassword
        String password,
        @NotBlank(message = "Password confirmation is required")
        String passwordConfirmation) {
}
