package org.tubalabs.app.security.remember;

import lombok.NonNull;

import java.util.UUID;

public final class RememberedLoginName {

    private RememberedLoginName() {
    }

    public static String username(@NonNull UUID identityId) {
        return identityId.toString();
    }

    public static UUID identityId(@NonNull String username) {
        return UUID.fromString(username);
    }
}
