package org.tubalabs.app.users.identity.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NonNull;
import org.tubalabs.app.users.identity.password.validation.SafePassword;

public record LocalUserRegistration(
        @NonNull
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.valid}")
        @Size(max = 320, message = "{validation.email.max-length}")
        String email,
        @NonNull
        @NotBlank(message = "{validation.password.required}")
        @SafePassword
        String password) {
}
