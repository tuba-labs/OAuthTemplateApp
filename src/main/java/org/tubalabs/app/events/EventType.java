package org.tubalabs.app.events;

import lombok.NonNull;

public enum EventType {
    USER_LOGIN("user.login"),
    USER_LOGOUT("user.logout"),
    SIGN_IN_METHOD_LINKED("sign_in_method.linked");

    private final String value;

    EventType(@NonNull String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
