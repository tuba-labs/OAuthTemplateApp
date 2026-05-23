(function () {
    "use strict";

    var storageKey = "oauth-template-app-theme";

    document.documentElement.dataset.theme = localStorage.getItem(storageKey) === "light" ? "light" : "dark";

    document.addEventListener("DOMContentLoaded", function () {
        var toggle = document.querySelector("[data-theme-toggle]");
        if (!toggle) {
            return;
        }

        function updateToggle() {
            var lightThemeActive = document.documentElement.dataset.theme === "light";
            toggle.setAttribute("aria-pressed", String(lightThemeActive));
            toggle.setAttribute("aria-label", lightThemeActive ? "Use dark theme" : "Use light theme");
            toggle.setAttribute("title", lightThemeActive ? "Use dark theme" : "Use light theme");
        }

        toggle.addEventListener("click", function () {
            var nextTheme = document.documentElement.dataset.theme === "light" ? "dark" : "light";
            document.documentElement.dataset.theme = nextTheme;
            localStorage.setItem(storageKey, nextTheme);
            updateToggle();
        });

        updateToggle();
    });
})();
