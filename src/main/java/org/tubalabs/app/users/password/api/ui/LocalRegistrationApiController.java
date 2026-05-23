package org.tubalabs.app.users.password.api.ui;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.tubalabs.app.users.password.CreateResult;
import org.tubalabs.app.users.password.LocalEmailNormalizer;
import org.tubalabs.app.users.password.LocalUserService;
import org.tubalabs.app.users.password.LocalUserRegistration;
import org.tubalabs.app.users.password.validation.SafePassword;
import org.tubalabs.app.users.profile.ValidDisplayName;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class LocalRegistrationApiController {

    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match";

    private final LocalUserService localUserService;
    private final LocalEmailNormalizer emailNormalizer;

    @PostMapping("/api/local-users")
    @ResponseStatus(HttpStatus.CREATED)
    public LocalRegistrationResponse register(@Valid @RequestBody LocalRegistrationRequest request) {
        if (!Objects.equals(request.password(), request.passwordConfirmation())) {
            throw new LocalApiValidationException(PASSWORD_MISMATCH_MESSAGE);
        }

        final String normalizedEmail = emailNormalizer.normalize(request.email());
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(
                normalizedEmail, request.password(), request.displayName()));
        if (createResult.vetoed()) {
            throw new LocalApiValidationException(createResult.firstVeto().englishReason());
        }
        return new LocalRegistrationResponse(createResult.id(), normalizedEmail);
    }

    public record LocalRegistrationRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 320, message = "Email must be 320 characters or fewer")
            String email,
            @NotBlank(message = "Password is required")
            @SafePassword
            String password,
            @NotBlank(message = "Password confirmation is required")
            String passwordConfirmation,
            @NotBlank(message = "Display name is required")
            @ValidDisplayName
            @Size(max = 80, message = "Display name must be 80 characters or fewer")
            String displayName) {
    }

    public record LocalRegistrationResponse(@NonNull UUID userId, @NonNull String email) {
    }
}
