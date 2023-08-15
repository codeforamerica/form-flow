document.addEventListener("DOMContentLoaded", function () {
  const allFollowUpTriggers = document.querySelectorAll('[data-follow-up]');

  allFollowUpTriggers.forEach(followUpTrigger => {
    if (followUpTrigger.checked === false) {
      const followUpInputs = document.querySelectorAll(
          `${document.querySelector(
              '[data-follow-up]').dataset.followUp} input`);
      followUpInputs.forEach(input => {
        input.setAttribute("disabled", "");
      });
    }
  });
});
