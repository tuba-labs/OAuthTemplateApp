package org.tubalabs.app.ui.startpage.menusystem;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.navigation.NavigationPageRegistration;
import org.tubalabs.app.navigation.PageModel;
import org.tubalabs.app.navigation.PageText;

@Component
public final class HomePage implements NavigationPageRegistration {

    public static final String RELATIVE_URL = "/";
    public static final String VIEW = "ui/startpage/home";
    private static final PageModel MODEL = new PageModel(new PageText(
            "navigation.page.home.label",
            "navigation.page.home.tooltip"), RELATIVE_URL);

    @Override
    public @NonNull PageModel model() {
        return MODEL;
    }

    @Override
    public int order() {
        return 100;
    }
}
