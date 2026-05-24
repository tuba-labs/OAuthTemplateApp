package org.tubalabs.app.users.identity;

import lombok.NonNull;

public class LoginTypeUnlinkException extends IllegalArgumentException {

    private final LoginTypeUnlinkFailure reason;

    public LoginTypeUnlinkException(@NonNull LoginTypeUnlinkFailure reason) {
        super("Login type unlink failed: " + reason);
        this.reason = reason;
    }

    public LoginTypeUnlinkFailure reason() {
        return reason;
    }
}
