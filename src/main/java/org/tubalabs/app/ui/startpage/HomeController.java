package org.tubalabs.app.ui.startpage;

import lombok.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.tubalabs.app.navigation.ui.AbstractNavigationController;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.ui.startpage.menusystem.HomePage;
import org.tubalabs.app.users.current.CurrentUserRequestContext;
import org.tubalabs.app.users.preferences.global.ui.GlobalUserPreferencesPageModel;

@Controller
public class HomeController extends AbstractNavigationController {

    public HomeController(@NonNull CurrentUserRequestContext currentUserRequestContext,
                          @NonNull NavigationPageModel navigationPageModel,
                          @NonNull GlobalUserPreferencesPageModel globalUserPreferencesPageModel) {
        super(currentUserRequestContext, navigationPageModel, globalUserPreferencesPageModel);
    }

    @GetMapping(HomePage.RELATIVE_URL)
    public String home() {
        return HomePage.VIEW;
    }
}
