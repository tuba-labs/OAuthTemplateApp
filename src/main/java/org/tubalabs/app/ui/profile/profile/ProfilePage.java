package org.tubalabs.app.ui.profile.profile;

import org.tubalabs.app.navigation.MainPage;
import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.ui.profile.logintypes.ProfileLoginTypesPage;
import org.tubalabs.app.ui.profile.changepassword.ProfileChangePasswordPage;

import java.util.List;

public final class ProfilePage {

    public static final String RELATIVE_URL = "/profile";
    public static final String REDIRECT = "redirect:" + RELATIVE_URL;
    public static final String VIEW = "ui/profile/profile/profile";
    public static final MainPage PAGE = new MainPage(new PageModel(
            "Profile",
            "Manage your profile",
            RELATIVE_URL), List.of(
                    ProfileLoginTypesPage.PAGE,
                    ProfileChangePasswordPage.PAGE));

    private ProfilePage() {
    }
}
