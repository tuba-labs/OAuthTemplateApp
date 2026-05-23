(function () {
    "use strict";

    var storageKey = "oauth-template-app-theme";

    document.documentElement.dataset.theme = localStorage.getItem(storageKey) === "light" ? "light" : "dark";

    document.addEventListener("DOMContentLoaded", function () {
        var toggles = document.querySelectorAll("[data-theme-toggle]");
        if (!toggles.length) {
            return;
        }

        function updateToggle() {
            var lightThemeActive = document.documentElement.dataset.theme === "light";
            toggles.forEach(function (toggle) {
                var label = toggle.querySelector("[data-theme-toggle-label]");
                toggle.setAttribute("aria-pressed", String(lightThemeActive));
                toggle.setAttribute("aria-label", lightThemeActive ? "Use dark theme" : "Use light theme");
                toggle.setAttribute("title", lightThemeActive ? "Use dark theme" : "Use light theme");
                toggle.dataset.themeToggleState = lightThemeActive ? "light" : "dark";
                if (label) {
                    label.textContent = lightThemeActive ? "Light" : "Dark";
                }
            });
        }

        toggles.forEach(function (toggle) {
            toggle.addEventListener("click", function () {
                var nextTheme = document.documentElement.dataset.theme === "light" ? "dark" : "light";
                document.documentElement.dataset.theme = nextTheme;
                localStorage.setItem(storageKey, nextTheme);
                updateToggle();
            });
        });

        updateToggle();
    });
})();
