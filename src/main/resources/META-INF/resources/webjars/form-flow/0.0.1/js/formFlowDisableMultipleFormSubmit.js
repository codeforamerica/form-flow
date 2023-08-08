document.addEventListener("DOMContentLoaded", function () {
  const formSubmitButton = document.getElementById("form-submit-button");
  if (formSubmitButton) {
    const form = formSubmitButton.form;
    if (form) {
      form.addEventListener("submit", function () {
        formSubmitButton.classList.add("button--disabled");
        formSubmitButton.disabled = true;
      });
    }
  }
});