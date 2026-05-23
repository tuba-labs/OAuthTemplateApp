(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var input = document.querySelector("[data-profile-picture-input]");
        var control = document.querySelector("[data-profile-picture-control]");
        if (!input || !control) {
            return;
        }

        var objectUrl = null;

        input.addEventListener("change", function () {
            var file = input.files && input.files[0];
            if (!file) {
                return;
            }

            if (objectUrl) {
                URL.revokeObjectURL(objectUrl);
            }
            objectUrl = URL.createObjectURL(file);

            var image = control.querySelector("[data-profile-picture-preview]");
            if (!image) {
                image = document.createElement("img");
                image.className = "profile-picture-image";
                image.alt = "";
                image.setAttribute("data-profile-picture-preview", "");
                control.prepend(image);
            }
            image.src = objectUrl;

            var placeholder = control.querySelector("[data-profile-picture-placeholder]");
            if (placeholder) {
                placeholder.remove();
            }
        });
    });
})();
