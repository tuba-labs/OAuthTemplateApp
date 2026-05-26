package org.tubalabs.app.navigation;

import lombok.NonNull;
import org.tubalabs.app.users.current.CurrentUser;

@FunctionalInterface
public interface PageAvailability {

    PageAvailability ALWAYS = currentUser -> true;

    boolean available(@NonNull CurrentUser currentUser);
}
