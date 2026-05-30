package org.tubalabs.app.email;

import lombok.NonNull;

public interface EmailSender {

    void send(@NonNull EmailMessage message);
}
