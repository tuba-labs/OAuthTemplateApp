package org.tubalabs.app.users.password.validation.vetoers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tubalabs.app.users.password.LocalUserRegistration;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCreateVetoerService {

    private final List<PasswordUserCreateVetoer> validators;

    public List<CreationVetoResult> getVetoes(@NonNull LocalUserRegistration registration) {
        return validators.stream()
                .map(f -> f.validate(registration))
                .filter(f -> !f.accepted())
                .toList();

    }
}
