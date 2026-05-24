package org.tubalabs.app.ui.startpage;

import org.tubalabs.app.navigation.MainPage;
import org.tubalabs.app.navigation.PageModel;

public final class HomePage {

    public static final String RELATIVE_URL = "/";
    public static final String VIEW = "startpage/home";
    public static final MainPage PAGE = new MainPage(
            new PageModel("Home", RELATIVE_URL));

    private HomePage() {
    }
}
