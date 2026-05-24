package org.tubalabs.app.users.profile.api.ui;

import lombok.NonNull;

public record ProfileLoginTypeOption(
        @NonNull String providerId,
        @NonNull String label) {
}
