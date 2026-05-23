package org.tubalabs.app.users.password.validation.vetoers;

import lombok.NonNull;
import org.tubalabs.app.users.password.LocalUserRegistration;


public interface PasswordUserCreateVetoer {

    @NonNull
    CreationVetoResult validate(@NonNull LocalUserRegistration localUserRegistration);


}
