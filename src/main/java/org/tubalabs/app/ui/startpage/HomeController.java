package org.tubalabs.app.ui.startpage;

import lombok.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.tubalabs.app.navigation.ui.AbstractNavigationController;
import org.tubalabs.app.navigation.ui.NavigationPageModel;
import org.tubalabs.app.ui.startpage.menusystem.HomePage;
import org.tubalabs.app.users.CurrentUserIdResolver;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

@Controller
public class HomeController extends AbstractNavigationController {

    public HomeController(@NonNull CurrentUserIdResolver currentUserIdResolver,
                          @NonNull CurrentUserSession currentUserSession,
                          @NonNull ProfileSetupRequirementService profileSetupRequirementService,
                          @NonNull NavigationPageModel navigationPageModel) {
        super(currentUserIdResolver, currentUserSession, profileSetupRequirementService, navigationPageModel);
    }

    @GetMapping(HomePage.RELATIVE_URL)
    public String home() {
        return HomePage.VIEW;
    }
}
