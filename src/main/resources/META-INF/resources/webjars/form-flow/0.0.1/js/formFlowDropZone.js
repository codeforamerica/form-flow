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
      $(document.getElementById(idToDisable)).prop( "disabled", false );
    });

    window[dropzonePrefix + inputName].on('removedfile', function () {
      if (window[dropzonePrefix + inputName].files.length === 0) {
        $(document.getElementById(idToDisable)).prop( "disabled", true );
      }
    });

    if (window[dropzonePrefix + inputName].files.length > 0) {
      $(document.getElementById(idToDisable)).prop( "disabled", false );
    }
  }
}

window.FormFlowDZ = FormFlowDZ;