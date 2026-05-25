package org.tubalabs.app.navigation;

import java.util.List;

public sealed interface NavigablePage permits MainPage, SubPage {

    PageModel model();

    List<SubPage> subPages();

    default String label() {
        return model().label();
    }

    default String tooltip() {
        return model().tooltip();
    }

    default String relativeUrl() {
        return model().relativeUrl();
    }

    default boolean visibleInNavigation() {
        return model().visibleInNavigation();
    }
}
