package org.tubalabs.app.users.identity.password.validation.vetoers;

import lombok.NonNull;
import org.tubalabs.app.users.identity.password.LocalUserRegistration;


public interface PasswordUserCreateVetoer {

    @NonNull
    CreationVetoResult validate(@NonNull LocalUserRegistration localUserRegistration);


}
