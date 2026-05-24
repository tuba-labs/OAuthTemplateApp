package org.tubalabs.app.ui.profile.changepassword;

import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.SubPage;
import org.tubalabs.app.ui.profile.profile.ProfilePage;

public final class ProfileChangePasswordPage {

    public static final String RELATIVE_URL = ProfilePage.RELATIVE_URL + "/password";
    public static final String VIEW = "users/profile/profile-password";
    public static final SubPage PAGE = new SubPage(new PageModel(
            "Change password",
            "Change your email login password",
            RELATIVE_URL));

    private ProfileChangePasswordPage() {
    }
}
