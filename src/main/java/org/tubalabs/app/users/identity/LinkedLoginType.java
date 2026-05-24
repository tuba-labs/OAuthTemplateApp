package org.tubalabs.app.users.identity;

import lombok.NonNull;

public record LinkedLoginType(
        @NonNull String providerId,
        boolean current,
        @NonNull LoginTypeUnlinkState unlinkState) {

    public boolean unlinkAvailable() {
        return unlinkState == LoginTypeUnlinkState.AVAILABLE;
    }
}
