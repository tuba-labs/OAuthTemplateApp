package org.tubalabs.app.users.identity.externalidentity;

import lombok.NonNull;

public class IdentityLinkException extends RuntimeException {

    private final IdentityLinkFailure reason;

    public IdentityLinkException(@NonNull IdentityLinkFailure reason) {
        super("Identity link failed: " + reason);
        this.reason = reason;
    }

    public IdentityLinkFailure reason() {
        return reason;
    }
}
