package org.tubalabs.app.ui.profile.logintypes.local;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.navigation.NavigationPageRegistration;
import org.tubalabs.app.navigation.PageAvailability;
import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.PageText;
import org.tubalabs.app.ui.profile.logintypes.menusystem.ProfileLoginTypesPage;
import org.tubalabs.app.users.current.CurrentUser;

import java.util.Optional;

@Component
public final class ProfileLocalLoginTypePage implements NavigationPageRegistration {

    public static final String RELATIVE_URL = ProfileLoginTypesPage.RELATIVE_URL + "/local/link";
    public static final String VIEW = "ui/profile/logintypes/local/profile-local-login-type";
    private static final PageAvailability AVAILABLE = ProfileLocalLoginTypePage::available;
    private static final PageModel MODEL = new PageModel(new PageText(
            "navigation.page.profile.login-types.link-local.label",
            "navigation.page.profile.login-types.link-local.tooltip"),
            RELATIVE_URL,
            false,
            AVAILABLE);

    @Override
    public @NonNull PageModel model() {
        return MODEL;
    }

    @Override
    public @NonNull Optional<String> parentRelativeUrl() {
        return Optional.of(ProfileLoginTypesPage.RELATIVE_URL);
    }

    @Override
    public int order() {
        return 100;
    }

    private static boolean available(@NonNull CurrentUser currentUser) {
        return !currentUser.profileSetupRequired() && currentUser.localLoginLinkAvailable();
    }
}
