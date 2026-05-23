package org.tubalabs.app.users.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NonNull;
import org.tubalabs.app.users.password.validation.SafePassword;

public record LocalUserRegistration(
        @NonNull
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must be 320 characters or fewer")
        String email,
        @NonNull
        @NotBlank(message = "Password is required")
        @SafePassword
        String password,
        @NonNull
        @NotBlank(message = "Display name is required")
        @Size(max = 80, message = "Display name must be 80 characters or fewer")
        String displayName) {
}
