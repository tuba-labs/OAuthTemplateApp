package org.tubalabs.app.navigation;

import java.util.List;

public sealed interface NavigablePage permits MainPage, SubPage {

    PageModel model();

    List<SubPage> subPages();

    default PageText text() {
        return model().text();
    }

    default String relativeUrl() {
        return model().relativeUrl();
    }

    default boolean visibleInNavigation() {
        return model().visibleInNavigation();
    }
}
