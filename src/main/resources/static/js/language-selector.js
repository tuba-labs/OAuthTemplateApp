(function () {
    "use strict";

    var storageKey = "oauth-template-app-language";
    var languageParameter = "language";

    function readStoredLanguage() {
        try {
            return localStorage.getItem(storageKey);
        } catch (exception) {
            return null;
        }
    }

    function writeStoredLanguage(languageTag) {
        try {
            localStorage.setItem(storageKey, languageTag);
        } catch (exception) {
            return;
        }
    }

    function optionExists(select, languageTag) {
        return Array.prototype.some.call(select.options, function (option) {
            return option.value === languageTag;
        });
    }

    function currentSelectedLanguage(select) {
        return select.value || "en";
    }

    function urlSelectedLanguage(select) {
        var url = new URL(window.location.href);
        var languageTag = url.searchParams.get(languageParameter);
        if (languageTag && optionExists(select, languageTag)) {
            return languageTag;
        }
        return null;
    }

    function redirectWithLanguage(languageTag) {
        var nextUrl = new URL(window.location.href);
        nextUrl.searchParams.set(languageParameter, languageTag);
        window.location.assign(nextUrl.toString());
    }

    function localStorageLanguage(select) {
        var currentLanguage = currentSelectedLanguage(select);
        var requestedLanguage = urlSelectedLanguage(select);
        if (requestedLanguage) {
            writeStoredLanguage(requestedLanguage);
            select.value = requestedLanguage;
            return;
        }

        var storedLanguage = readStoredLanguage();
        if (!storedLanguage || !optionExists(select, storedLanguage)) {
            storedLanguage = currentLanguage;
            writeStoredLanguage(storedLanguage);
        }

        if (storedLanguage !== currentLanguage) {
            redirectWithLanguage(storedLanguage);
            return;
        }
        select.value = storedLanguage;
    }

    function submitDatabaseLanguage(form) {
        var returnTo = form.querySelector("[data-language-return-to]");
        if (returnTo) {
            returnTo.value = window.location.pathname + window.location.search;
        }
        form.submit();
    }

    document.addEventListener("DOMContentLoaded", function () {
        var forms = document.querySelectorAll("[data-language-selector-form]");
        forms.forEach(function (form) {
            var select = form.querySelector("[data-language-selector]");
            if (!select) {
                return;
            }

            if (form.dataset.languagePersistence === "local") {
                localStorageLanguage(select);
            }

            select.addEventListener("change", function () {
                if (form.dataset.languagePersistence === "local") {
                    writeStoredLanguage(select.value);
                    redirectWithLanguage(select.value);
                    return;
                }
                submitDatabaseLanguage(form);
            });
        });
    });
})();
