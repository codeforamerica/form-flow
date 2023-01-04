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
  }
}

window.FormFlowDZ = FormFlowDZ;