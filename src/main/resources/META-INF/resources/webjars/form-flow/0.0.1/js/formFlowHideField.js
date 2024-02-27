function toggleVisibility(checkbox, selector) {
  let element = document.querySelector(selector);
  if (element) {
    element.style.display = checkbox.checked ? 'none' : 'block';
  }
}

document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll('input[type="checkbox"][onchange^="toggleVisibility"]').forEach(checkbox => {
    const fieldSelector = checkbox.getAttribute('onchange').match(/toggleVisibility\(this, '(.+)'\)/)[1];
    toggleVisibility(checkbox, fieldSelector);
  });
});
