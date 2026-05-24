package org.tubalabs.app.users.identity.password.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.tubalabs.app.users.identity.password.CreateResult;
import org.tubalabs.app.users.identity.password.LocalEmailNormalizer;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.identity.password.LocalUserRegistration;
import org.tubalabs.app.users.identity.password.api.dtos.LocalRegistrationRequestDto;
import org.tubalabs.app.users.identity.password.api.dtos.LocalRegistrationResponseDto;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class LocalRegistrationApiController {

    private static final String PASSWORD_MISMATCH_MESSAGE = "Passwords do not match";

    private final LocalUserService localUserService;
    private final LocalEmailNormalizer emailNormalizer;

    @PostMapping("/api/local-users")
    @ResponseStatus(HttpStatus.CREATED)
    public LocalRegistrationResponseDto register(@Valid @RequestBody LocalRegistrationRequestDto request) {
        if (!Objects.equals(request.password(), request.passwordConfirmation())) {
            throw new LocalApiValidationException(PASSWORD_MISMATCH_MESSAGE);
        }

        final String normalizedEmail = emailNormalizer.normalize(request.email());
        final CreateResult createResult = localUserService.register(new LocalUserRegistration(
                normalizedEmail, request.password()));
        if (createResult.vetoed()) {
            throw new LocalApiValidationException(createResult.firstVeto().englishReason());
        }
        return new LocalRegistrationResponseDto(createResult.id(), normalizedEmail);
    }
}
