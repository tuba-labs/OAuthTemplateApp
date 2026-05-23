package org.tubalabs.app.users.password;

import lombok.Builder;
import lombok.NonNull;
import org.tubalabs.app.users.password.validation.vetoers.CreationVetoResult;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record CreateResult(UUID id, List<CreationVetoResult> creationVetoed) {

    public CreateResult {
        creationVetoed = creationVetoed == null ? List.of() : List.copyOf(creationVetoed);
        if (id != null && !creationVetoed.isEmpty()) {
            throw new IllegalArgumentException("Create result cannot contain both a user id and creation vetoes");
        }
        if (id == null && creationVetoed.isEmpty()) {
            throw new IllegalArgumentException("Create result must contain either a user id or creation vetoes");
        }
    }

    public static CreateResult created(@NonNull UUID id) {
        return new CreateResult(id, List.of());
    }

    public static CreateResult vetoed(@NonNull List<CreationVetoResult> vetoes) {
        return new CreateResult(null, vetoes);
    }

    public boolean vetoed() {
        return !creationVetoed.isEmpty();
    }

    public CreationVetoResult firstVeto() {
        if (!vetoed()) {
            throw new IllegalStateException("Create result does not contain vetoes");
        }
        return creationVetoed.get(0);
    }
}
