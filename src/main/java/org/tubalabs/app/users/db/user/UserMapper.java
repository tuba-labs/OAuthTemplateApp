package org.tubalabs.app.users.db.user;

import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserMapper {

    public UserDbo newUser(@NonNull UUID userId) {
        return UserDbo.builder()
                .id(userId)
                .build();
    }
}