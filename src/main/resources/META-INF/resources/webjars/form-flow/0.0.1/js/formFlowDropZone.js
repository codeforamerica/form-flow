FormFlowDZ = {
  hideContinueIfNoFiles: function (inputName, idToHide) {
    window[dropzonePrefix + inputName].on('success', function () {
      $(document.getElementById(idToHide)).removeClass("display-none");
    });

    window[dropzonePrefix + inputName].on('removedfile', function () {
      if (window[dropzonePrefix + inputName].files.length === 0) {
        $(document.getElementById(idToHide)).addClass("display-none");
      }
    });

    if (window[dropzonePrefix + inputName].files.length > 0) {
      $(document.getElementById(idToHide)).removeClass("display-none");
    }
  },
    disableIfNoFiles: function (inputName, idToDisable) {
      window[dropzonePrefix + inputName].on('success', function () {
        $(document.getElementById(idToDisable)).removeClass("disabled");
      });

      window[dropzonePrefix + inputName].on('removedfile', function () {
        if (window[dropzonePrefix + inputName].files.length === 0) {
          $(document.getElementById(idToDisable)).addClass("disabled");
        }
      });

      if (window[dropzonePrefix + inputName].files.length > 0) {
        $(document.getElementById(idToDisable)).removeClass("disabled");
      }
  }
}

window.FormFlowDZ = FormFlowDZ;