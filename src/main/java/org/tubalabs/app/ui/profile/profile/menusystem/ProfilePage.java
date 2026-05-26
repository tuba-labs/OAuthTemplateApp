package org.tubalabs.app.ui.profile.profile.menusystem;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.navigation.NavigationPageRegistration;
import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.PageText;

@Component
public final class ProfilePage implements NavigationPageRegistration {

    public static final String RELATIVE_URL = "/profile";
    public static final String REDIRECT = "redirect:" + RELATIVE_URL;
    public static final String VIEW = "ui/profile/profile/profile";
    private static final PageModel MODEL = new PageModel(new PageText(
            "navigation.page.profile.label",
            "navigation.page.profile.tooltip"), RELATIVE_URL);

    @Override
    public @NonNull PageModel model() {
        return MODEL;
    }

    @Override
    public int order() {
        return 200;
    }
}
