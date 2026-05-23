package org.tubalabs.app.users.identity.password.validation.vetoers;

import lombok.Builder;

@Builder(toBuilder = true)
public record CreationVetoResult(boolean accepted, String id, String englishReason) {
    public CreationVetoResult {
        if (!accepted) {
            if (id == null || id.isBlank() || englishReason == null || englishReason.isBlank()) {
                throw new IllegalArgumentException("Negative results require a englishReason and id");
            }
        } else {
            if (id != null || englishReason != null) {
                throw new IllegalArgumentException("Positive results can not have an id or englishReason");
            }
        }
    }

    public static CreationVetoResult ok() {
        return new CreationVetoResult(true, null, null);
    }


    public static CreationVetoResult stop(String id, String englishReason) {
        return new CreationVetoResult(false, id, englishReason);
    }
}
