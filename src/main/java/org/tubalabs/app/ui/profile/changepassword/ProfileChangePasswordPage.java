package org.tubalabs.app.ui.profile.changepassword;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.navigation.NavigationPageRegistration;
import org.tubalabs.app.navigation.PageAvailability;
import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.PageText;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePage;
import org.tubalabs.app.users.current.CurrentUser;

import java.util.Optional;

@Component
public final class ProfileChangePasswordPage implements NavigationPageRegistration {

    public static final String RELATIVE_URL = ProfilePage.RELATIVE_URL + "/password";
    public static final String VIEW = "ui/profile/changepassword/profile-password";
    private static final PageAvailability AVAILABLE = CurrentUser::passwordChangeAvailable;
    private static final PageModel MODEL = new PageModel(new PageText(
            "navigation.page.profile.change-password.label",
            "navigation.page.profile.change-password.tooltip"), RELATIVE_URL, false, AVAILABLE);

    @Override
    public @NonNull PageModel model() {
        return MODEL;
    }

    @Override
    public @NonNull Optional<String> parentRelativeUrl() {
        return Optional.of(ProfilePage.RELATIVE_URL);
    }

    @Override
    public int order() {
        return 200;
    }
}
