package org.tubalabs.app.navigation;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.tubalabs.app.ui.startpage.HomePage;
import org.tubalabs.app.ui.profile.profile.ProfilePage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class NavigationCatalog {

    private static final List<MainPage> MAIN_PAGES = List.of(
            HomePage.PAGE,
            ProfilePage.PAGE);

    public List<MainPage> mainPages() {
        return MAIN_PAGES;
    }

    public Optional<NavigablePage> activePage(@NonNull String currentPath) {
        return MAIN_PAGES.stream()
                .flatMap(this::pageAndSubPages)
                .filter(page -> matches(currentPath, page.relativeUrl()))
                .max(Comparator.comparingInt(page -> page.relativeUrl().length()));
    }

    private Stream<NavigablePage> pageAndSubPages(@NonNull NavigablePage page) {
        return Stream.concat(
                Stream.of(page),
                page.subPages().stream().flatMap(this::pageAndSubPages));
    }

    private boolean matches(@NonNull String currentPath, @NonNull String relativeUrl) {
        if (relativeUrl.isBlank()) {
            return false;
        }
        if ("/".equals(relativeUrl)) {
            return "/".equals(currentPath);
        }
        return currentPath.equals(relativeUrl) || currentPath.startsWith(relativeUrl + "/");
    }
}
