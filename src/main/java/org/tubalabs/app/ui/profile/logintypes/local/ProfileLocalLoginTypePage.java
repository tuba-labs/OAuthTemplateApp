package org.tubalabs.app.ui.profile.logintypes.local;

import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.SubPage;
import org.tubalabs.app.ui.profile.logintypes.ProfileLoginTypesPage;

public final class ProfileLocalLoginTypePage {

    public static final String RELATIVE_URL = ProfileLoginTypesPage.RELATIVE_URL + "/local/link";
    public static final String VIEW = "ui/profile/logintypes/local/profile-local-login-type";
    public static final SubPage PAGE = new SubPage(new PageModel(
            "Link email and password",
            "Add email and password login",
            RELATIVE_URL,
            false));

    private ProfileLocalLoginTypePage() {
    }
}
