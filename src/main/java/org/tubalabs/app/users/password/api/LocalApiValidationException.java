package org.tubalabs.app.users.password.api;

public class LocalApiValidationException extends IllegalArgumentException {

    public LocalApiValidationException(String message) {
        super(message);
    }
}
