package org.tubalabs.app.users.identity.password.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.tubalabs.app.users.identity.password.validation.SafePassword;

public record LocalRegistrationRequestDto(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.valid}")
        @Size(max = 320, message = "{validation.email.max-length}")
        String email,
        @NotBlank(message = "{validation.password.required}")
        @SafePassword
        String password,
        @NotBlank(message = "{validation.password-confirmation.required}")
        String passwordConfirmation) {
}
