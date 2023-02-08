QUnit.module('Dropzone', function () {
  QUnit.testStart(function () {
    window['myDropZoneuploadTest'].removeAllFiles();
  });
  QUnit.module('Configuration options', function () {
    QUnit.test('loads accepted files from application.yaml', assert => {
      assert.equal(window['myDropZoneuploadTest'].options.acceptedFiles,
          ".jpeg, .fake, .heic, .tif, .tiff");
    });

    QUnit.test('loads max file size from application.yaml', assert => {
      assert.equal(window['myDropZoneuploadTest'].options.maxFilesize, "17");
    });

    QUnit.test('loads max number of files from application.yaml', assert => {
      assert.equal(window['myDropZoneuploadTest'].options.maxFiles, "5");
    });

    QUnit.test('loads thumbnail height/width from application.yaml', assert => {
      assert.equal(window['myDropZoneuploadTest'].options.thumbnailHeight,
          "50");
      assert.equal(window['myDropZoneuploadTest'].options.thumbnailWidth,
          "100");
    });
  });
  QUnit.module('Invalid file types', function () {
    QUnit.test(
        'HEIC files throw an error even when allowed to be uploaded',
        assert => {
          var heicFile = new File([""], "test.heic", {type: "image/heic"});
          window['myDropZoneuploadTest'].addFile(heicFile);
          assert.equal(
              document.getElementsByClassName('text--error')[0].innerHTML,
              "We are unable to process HEIC files. Please convert your file to a JPG or PNG and try again.")
        });
    QUnit.test('TIFF files throw an error even when allowed to be uploaded',
        assert => {
          var tiffFile = new File([""], "test.tiff", {type: "image/tiff"});
          window['myDropZoneuploadTest'].addFile(tiffFile);
          assert.equal(
              document.getElementsByClassName('text--error')[0].innerHTML,
              "We are unable to process TIFF files. Please convert your file to a JPG or PNG and try again.")
        });
  });
});
QUnit.testDone(function () {
  document.querySelector("#uploadTest").className += " display-none";
});