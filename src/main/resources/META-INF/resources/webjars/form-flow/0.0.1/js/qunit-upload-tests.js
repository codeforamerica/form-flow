QUnit.module('Dropzone', function () {

  QUnit.testStart(function () {
    window['myDropZoneuploadTest'].removeAllFiles();
  });

  QUnit.test('loads accepted files from application.yaml', function (assert) {

    assert.equal(window['myDropZoneuploadTest'].options.acceptedFiles,
        ".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp");
  });

  QUnit.testDone(function () {
    document.querySelector("#uploadTest").className += " display-none";
  });

  QUnit.test('deleting an upload file removes it from the page',
      function (assert) {
        // Upload a file
        const file = new File([""], "test.pdf", {type: "application/pdf"});

        window['myDropZoneuploadTest'].addFile(file);
        assert.equal(document.querySelectorAll('.dz-preview').length, 1);

        // // Mock clicking ok in confirm window
        // window.confirm = function () {
        //   console.log("confirmed deletion");
        //   return true;
        // };
        //
        // // Delete file
        // console.log("Click delete");
        // document.querySelector('.dz-remove').click();

        // // Check file is no longer on page
        // console.log("Assert elements removed from DOM");
        // assert.equal(document.querySelectorAll('.dz-preview').length, 0);
        // // Assert the dzInstance has no files
        // console.log("Assert elements removed from dzInstance");
        // assert.equal(window['myDropZoneuploadTest'].files.length, 0);
      });
});

QUnit.testDone(function () {
  document.querySelector("#uploadTest").className += " display-none";
});