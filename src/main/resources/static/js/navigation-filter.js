(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var form = document.querySelector("[data-navigation-filter-form]");
        var filter = document.querySelector("[data-navigation-filter]");
        var menu = document.querySelector("[data-navigation-menu]");
        var navigation = document.querySelector("[data-section-navigation]");
        var menuToggle = document.querySelector("[data-navigation-menu-toggle]");
        if (!form || !filter || !menu) {
            return;
        }

        if (navigation && menuToggle) {
            initializeNavigationMenu(navigation, menuToggle, filter, menu);
        }

        filter.addEventListener("input", function () {
            filterMenu(menu, filter.value);
        });
        filterMenu(menu, filter.value);

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            var firstLink = firstVisibleMatchingLink(menu) || firstVisibleLink(menu);
            if (firstLink) {
                window.location.assign(firstLink.href);
            }
        });
    });

    var openStorageKey = "oauth-template-app-navigation-open";

    function initializeNavigationMenu(navigation, menuToggle, filter, menu) {
        var shell = navigation.closest("[data-navigation-shell]");
        setNavigationOpen(navigation, menuToggle, shell, readOpenState());

        menuToggle.addEventListener("click", function () {
            var open = navigation.dataset.navigationOpen !== "true";
            navigation.dataset.navigationTransientOpen = "false";
            syncNavigationShell(shell, navigation);
            setNavigationOpen(navigation, menuToggle, shell, open);
            writeOpenState(open);
        });

        filter.addEventListener("focus", function () {
            setNavigationTransientOpen(navigation, menuToggle, shell, true);
        });
        filter.addEventListener("blur", function () {
            updateTransientOpenAfterFocusChange(navigation, menuToggle, shell, menu);
        });
        menu.addEventListener("focusin", function () {
            setNavigationTransientOpen(navigation, menuToggle, shell, true);
        });
        menu.addEventListener("focusout", function () {
            updateTransientOpenAfterFocusChange(navigation, menuToggle, shell, menu);
        });
        menu.addEventListener("mousedown", function () {
            setNavigationTransientOpen(navigation, menuToggle, shell, true);
        });
    }

    function setNavigationOpen(navigation, menuToggle, shell, open) {
        navigation.dataset.navigationOpen = String(open);
        syncNavigationShell(shell, navigation);
        updateMenuToggle(navigation, menuToggle);
    }

    function setNavigationTransientOpen(navigation, menuToggle, shell, transientOpen) {
        navigation.dataset.navigationTransientOpen = String(transientOpen);
        syncNavigationShell(shell, navigation);
        updateMenuToggle(navigation, menuToggle);
    }

    function updateTransientOpenAfterFocusChange(navigation, menuToggle, shell, menu) {
        window.setTimeout(function () {
            var activeElement = document.activeElement;
            var transientOpen = activeElement === navigation.querySelector("[data-navigation-filter]")
                    || menu.contains(activeElement);
            setNavigationTransientOpen(navigation, menuToggle, shell, transientOpen);
        }, 0);
    }

    function syncNavigationShell(shell, navigation) {
        document.documentElement.dataset.navigationOpen =
                String(navigation.dataset.navigationOpen === "true");
        if (!shell) {
            return;
        }
        shell.dataset.navigationOpen = String(navigation.dataset.navigationOpen === "true");
        shell.dataset.navigationTransientOpen = String(navigation.dataset.navigationTransientOpen === "true");
    }

    function updateMenuToggle(navigation, menuToggle) {
        var toggledOpen = navigation.dataset.navigationOpen === "true";
        var visible = toggledOpen || navigation.dataset.navigationTransientOpen === "true";
        var label = toggledOpen
                ? menuToggle.dataset.navigationCloseLabel
                : menuToggle.dataset.navigationOpenLabel;
        menuToggle.setAttribute("aria-pressed", String(toggledOpen));
        menuToggle.setAttribute("aria-expanded", String(visible));
        menuToggle.setAttribute("aria-label", label);
        menuToggle.setAttribute("title", label);
    }

    function readOpenState() {
        try {
            return localStorage.getItem(openStorageKey) === "true";
        } catch (exception) {
            return false;
        }
    }

    function writeOpenState(open) {
        try {
            localStorage.setItem(openStorageKey, String(open));
        } catch (exception) {
            return;
        }
    }

    function filterMenu(menu, value) {
        var query = value.trim().toLocaleLowerCase();
        var rootItems = menu.querySelectorAll(":scope > .app-menu-tree > [data-navigation-menu-item]");

        rootItems.forEach(function (item) {
            updateItemVisibility(item, query);
        });
    }

    function updateItemVisibility(item, query) {
        var ownLabelMatches = itemLabel(item).includes(query);
        var childMatches = false;
        var childItems = item.querySelectorAll(":scope > .app-menu-tree > [data-navigation-menu-item]");

        childItems.forEach(function (childItem) {
            childMatches = updateItemVisibility(childItem, query) || childMatches;
        });

        var visible = query === "" || ownLabelMatches || childMatches;
        item.dataset.navigationFilterMatch = String(ownLabelMatches);
        item.dataset.navigationFilterInherited = String(query !== "" && !ownLabelMatches && childMatches);
        item.hidden = !visible;
        return visible;
    }

    function itemLabel(item) {
        var label = item.querySelector(":scope > [data-navigation-menu-label]");
        if (!label) {
            return "";
        }
        return label.textContent.trim().toLocaleLowerCase();
    }

    function firstVisibleMatchingLink(menu) {
        var items = menu.querySelectorAll("[data-navigation-menu-item]:not([hidden])");
        for (var itemIndex = 0; itemIndex < items.length; itemIndex += 1) {
            if (items[itemIndex].dataset.navigationFilterMatch === "true") {
                var link = items[itemIndex].querySelector(":scope > .app-menu-link[href]");
                if (link) {
                    return link;
                }
            }
        }
        return null;
    }

    function firstVisibleLink(menu) {
        var links = menu.querySelectorAll(".app-menu-link[href]");
        for (var linkIndex = 0; linkIndex < links.length; linkIndex += 1) {
            if (!links[linkIndex].closest("[data-navigation-menu-item][hidden]")) {
                return links[linkIndex];
            }
        }
        return null;
    }
}());
