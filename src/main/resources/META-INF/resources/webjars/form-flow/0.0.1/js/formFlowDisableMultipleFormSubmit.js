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

window.addEventListener('pageshow', function (event) {
  if (event.persisted) {
    console.log("pageshow event: the page was restored from the bfcache" + event.persisted)
    const formSubmitButton = document.getElementById("form-submit-button");
    if (formSubmitButton) {
      formSubmitButton.classList.remove("button--disabled");
      formSubmitButton.disabled = false;
    }
  }
});
