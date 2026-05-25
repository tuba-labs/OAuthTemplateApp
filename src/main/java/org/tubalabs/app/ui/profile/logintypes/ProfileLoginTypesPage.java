package org.tubalabs.app.ui.profile.logintypes;

import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.SubPage;
import org.tubalabs.app.ui.profile.logintypes.local.ProfileLocalLoginTypePage;
import org.tubalabs.app.ui.profile.profile.ProfilePage;

import java.util.List;

public final class ProfileLoginTypesPage {

    public static final String RELATIVE_URL = ProfilePage.RELATIVE_URL + "/login-types";
    public static final String REDIRECT = "redirect:" + RELATIVE_URL;
    public static final String VIEW = "ui/profile/logintypes/profile-login-types";
    public static final SubPage PAGE = new SubPage(new PageModel(
            "Login types",
            "Manage login types",
            RELATIVE_URL), List.of(ProfileLocalLoginTypePage.PAGE));

    private ProfileLoginTypesPage() {
    }
}
