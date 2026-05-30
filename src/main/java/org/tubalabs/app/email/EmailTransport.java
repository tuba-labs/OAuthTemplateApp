package org.tubalabs.app.email;

import lombok.NonNull;

import java.util.UUID;

public interface EmailTransport {

    void deliver(@NonNull UUID emailId, @NonNull EmailMessage message);
}
