QUnit.module('Dropzone', function () {
  QUnit.testStart(function () {
    window['myDropZoneuploadTest'].removeAllFiles();
  });

  QUnit.test('loads accepted files from application.yaml', assert => {
    assert.equal(window['myDropZoneuploadTest'].options.acceptedFiles,
        ".jpeg, .fake");
  });

  QUnit.test('loads max file size from application.yaml', assert => {
    assert.equal(window['myDropZoneuploadTest'].options.maxFilesize, "17");
  });

  QUnit.test('loads max number of files from application.yaml', assert => {
    assert.equal(window['myDropZoneuploadTest'].options.maxFiles, "5");
  });

  QUnit.test('loads thumbnail height/width from application.yaml', assert => {
    assert.equal(window['myDropZoneuploadTest'].options.thumbnailHeight, "50");
    assert.equal(window['myDropZoneuploadTest'].options.thumbnailWidth, "100");
  });
});
QUnit.testDone(function () {
  document.querySelector("#uploadTest").className += " display-none";
});