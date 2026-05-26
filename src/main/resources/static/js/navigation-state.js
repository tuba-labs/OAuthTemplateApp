(function () {
    "use strict";

    var storageKey = "oauth-template-app-navigation-open";

    try {
        document.documentElement.dataset.navigationOpen =
                localStorage.getItem(storageKey) === "true" ? "true" : "false";
    } catch (exception) {
        document.documentElement.dataset.navigationOpen = "false";
    }
}());
