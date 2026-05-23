package org.tubalabs.app.users.password.validation.vetoers.password;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.tubalabs.app.etc.spring.conditionals.FeatureByClassNameEnabled;
import org.tubalabs.app.users.password.LocalUserRegistration;
import org.tubalabs.app.users.password.validation.vetoers.PasswordUserCreateVetoer;
import org.tubalabs.app.users.password.validation.vetoers.CreationVetoResult;

@Component
@RequiredArgsConstructor
@FeatureByClassNameEnabled
public class PwnedPasswordPasswordUserCreateVetoer implements PasswordUserCreateVetoer {

    private final PwnedPasswordClient pwnedPasswordClient;

    @Override
    public @NonNull CreationVetoResult validate(@NonNull LocalUserRegistration localUserRegistration) {
        final String value = localUserRegistration.password();

        if (pwnedPasswordClient.isPwned(value)) {
            return CreationVetoResult.stop("pwnedPsw",
                    "This password has appeared in a known data breach. Choose a different password.");
        }
        return CreationVetoResult.ok();
    }


}
