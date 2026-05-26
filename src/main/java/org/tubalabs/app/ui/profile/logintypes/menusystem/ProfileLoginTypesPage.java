package org.tubalabs.app.ui.profile.logintypes.menusystem;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.navigation.NavigationPageRegistration;
import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.PageText;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePage;

import java.util.Optional;

@Component
public final class ProfileLoginTypesPage implements NavigationPageRegistration {

    public static final String RELATIVE_URL = ProfilePage.RELATIVE_URL + "/login-types";
    public static final String REDIRECT = "redirect:" + RELATIVE_URL;
    public static final String VIEW = "ui/profile/logintypes/profile-login-types";
    private static final PageModel MODEL = new PageModel(new PageText(
            "navigation.page.profile.login-types.label",
            "navigation.page.profile.login-types.tooltip"), RELATIVE_URL);

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
        return 100;
    }
}
