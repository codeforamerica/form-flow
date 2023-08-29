package formflow.library.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "form-flow.path=flows-config/test-upload-flow.yaml",
    "form-flow.uploads.virus-scanning.enabled=false",
}, webEnvironment = RANDOM_PORT)
public class UploadJourneyTests extends AbstractBasePageTest {

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "flow/uploadFlow/docUploadJourney";
    super.setUp();
  }

  @Test
  void documentUploadFlow() {
    assertThat(testPage.getTitle()).isEqualTo("Upload Documents");
    // Test accepted file types
    // Extension list comes from application.yaml -- form-flow.uploads.accepted-file-types
    uploadFile("test-platypus.gif", "uploadTest");
    assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(messageSource
            .getMessage("upload-documents.error-invalid-file-type", null, Locale.ENGLISH)
            + " .jpeg, .pdf");
    testPage.clickLink("remove");
    assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");

    // Upload a file that is too big and assert the correct error shows - max file size in test is 1MB
    long largeFilesize = 21000000L;
    driver.executeScript(
        "$('#document-upload-uploadTest').get(0).dropzone.addFile({name: 'testFile.pdf', size: "
            + largeFilesize + ", type: 'not-an-image'})");
    int maxFileSize = 17;
    assertThat(driver.findElement(By.className("text--error")).getText()).contains(messageSource
        .getMessage("upload-documents.this-file-is-too-large", new Object[]{maxFileSize}, Locale.ENGLISH));
    testPage.clickLink("remove");
    assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");

    // Upload a password-protected file and assert the correct error shows
    uploadPasswordProtectedPdf("uploadTest");

    //Race condition caused by uploadPasswordProtectedPdf waits until upload file has file details added instead
    //of waiting until file upload is complete.
    //TODO: Change uploadFile() to wait until file upload is complete.  Key off of delete link?
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(messageSource.getMessage("upload-documents.error-password-protected", null, Locale.ENGLISH));
    testPage.clickLink("remove");
    assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");

    // Test max number of files that can be uploaded
    uploadJpgFile("uploadTest");
    assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("1 file added");
    uploadJpgFile("uploadTest"); // 2
    uploadJpgFile("uploadTest"); // 3
    uploadJpgFile("uploadTest"); // 4
    uploadJpgFile("uploadTest"); // 5
    uploadJpgFile("uploadTest"); // Can't upload the 6th
    assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(messageSource.getMessage("upload-documents.error-maximum-number-of-files", null, Locale.ENGLISH));
    testPage.clickLink("remove");
    // Assert there are no longer any error after removing the errored item
    assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("5 files added");
    assertThat(
        testPage.findElementsByClass("text--error").stream().map(WebElement::getText).collect(Collectors.toList()))
        .allMatch(String::isEmpty);
    // Delete all elements on the page and then assert there are no longer any uploaded files on the page
    while (!testPage.findElementsByClass("dz-remove").isEmpty()) {
      testPage.clickLink("delete");
      driver.switchTo().alert().accept();
    }
    assertThat(testPage.findElementsByClass("dz-remove").size()).isEqualTo(0);
    assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");
  }
}
