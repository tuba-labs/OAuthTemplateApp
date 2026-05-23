(function () {
    document.addEventListener("DOMContentLoaded", function () {
        var dialog = document.querySelector("[data-password-dialog]");
        var openButtons = document.querySelectorAll("[data-password-dialog-open]");
        if (!dialog || typeof dialog.showModal !== "function") {
            return;
        }

        openButtons.forEach(function (openButton) {
            openButton.addEventListener("click", function () {
                var menu = openButton.closest("details");
                if (menu) {
                    menu.open = false;
                }
                dialog.showModal();
            });
        });

        if (dialog.dataset.passwordDialogOpen === "true") {
            dialog.showModal();
        }
    });
}());
