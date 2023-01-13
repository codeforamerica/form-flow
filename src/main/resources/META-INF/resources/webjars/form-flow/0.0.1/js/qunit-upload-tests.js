
QUnit.module('Dropzone', function () {
  const testFile = new File([""], "test.pdf", {type: "application/pdf"});

  QUnit.testStart(function () {
    window['myDropZoneuploadTest'].removeAllFiles();
  });

  QUnit.test('loads accepted files from application.yaml', assert => {
    assert.equal(window['myDropZoneuploadTest'].options.acceptedFiles,
        ".jpeg,.jpg,.png,.pdf,.bmp,.gif,.doc,.docx,.odt,.ods,.odp");
  });

  QUnit.test('deleting an upload file removes it from the page', assert => {
    const uploadComplete = assert.async();
    const deleteComplete = assert.async();

    window['myDropZoneuploadTest'].on('success', () => {
      assert.equal(document.querySelector('.dz-remove').innerHTML, 'delete');
      document.querySelector('.dz-remove').click();
      window.confirm = () => true;
      uploadComplete();
    });

    window['myDropZoneuploadTest'].on('removedFile', () => {
      assert.equal(document.querySelectorAll('.dz-preview').length, 0);
      assert.equal(window['myDropZoneuploadTest'].files.length, 0);

      deleteComplete();
    });


    window['myDropZoneuploadTest'].addFile(testFile);
  });

  // QUnit.test('cancelling a file not fully uploaded removes it from the page', assert => {
  //   window['myDropZoneuploadTest'].addFile(testFile);
  //   assert.equal(document.querySelector('.dz-remove').innerHTML, 'cancel');
  //   document.querySelector('.dz-remove').click();
  //   assert.equal(document.querySelectorAll('.dz-preview').length, 0);
  //   assert.equal(window['myDropZoneuploadTest'].files.length, 0);
  //
  //   // assert.equal(document.querySelectorAll('.dz-preview').length, 1);
  //   // assert.equal(document.querySelector('.dz-remove').innerHTML, 'delete');
  //   // window.confirm = () => true;
  //   //
  //   // assert.equal(document.querySelectorAll('.dz-preview').length, 0);
  //   // assert.equal(window['myDropZoneuploadTest'].files.length, 0);
  // });


  // QUnit.test('clicking remove removes testFile from the page', assert => {
  // });
});

QUnit.testDone(function () {
  document.querySelector("#uploadTest").className += " display-none";
});