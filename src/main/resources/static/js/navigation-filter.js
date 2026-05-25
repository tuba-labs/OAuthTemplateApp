(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var form = document.querySelector("[data-navigation-filter-form]");
        var filter = document.querySelector("[data-navigation-filter]");
        var menu = document.querySelector("[data-navigation-menu]");
        if (!form || !filter || !menu) {
            return;
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
