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

// This prevents the button from being disabled when the user navigates back to 
// the page after submitting the form using back button. 
// (Safari caches the page in it's back-forward cache).
window.addEventListener('pageshow', function (event) {
  if (event.persisted) {
    const formSubmitButton = document.getElementById("form-submit-button");
    if (formSubmitButton) {
      formSubmitButton.classList.remove("button--disabled");
      formSubmitButton.disabled = false;
    }
  }
});
