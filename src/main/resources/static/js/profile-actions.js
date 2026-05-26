(function () {
    document.addEventListener("DOMContentLoaded", function () {
        wireConfirmationDialog();
    });

    function wireConfirmationDialog() {
        var dialog = document.querySelector("[data-confirm-dialog]");
        var title = document.querySelector("[data-confirm-dialog-title]");
        var submitButton = document.querySelector("[data-confirm-dialog-submit]");
        var forms = document.querySelectorAll("form[data-confirm-title]");
        var pendingForm = null;
        var defaultTitle = title ? title.textContent : "";
        var defaultAction = submitButton ? submitButton.textContent : "";

        if (!dialog || !title || !submitButton || typeof dialog.showModal !== "function") {
            return;
        }

        forms.forEach(function (form) {
            form.addEventListener("submit", function (event) {
                if (form.dataset.confirmed === "true") {
                    delete form.dataset.confirmed;
                    return;
                }

                event.preventDefault();
                pendingForm = form;
                title.textContent = form.dataset.confirmTitle || defaultTitle;
                submitButton.textContent = form.dataset.confirmAction || defaultAction;

                dialog.showModal();
            });
        });

        submitButton.addEventListener("click", function () {
            if (!pendingForm) {
                dialog.close();
                return;
            }

            var form = pendingForm;
            pendingForm = null;
            form.dataset.confirmed = "true";
            dialog.close();
            form.requestSubmit();
        });

        dialog.addEventListener("close", function () {
            pendingForm = null;
        });
    }
}());
