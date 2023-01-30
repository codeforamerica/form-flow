package formflow.library.upload;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-upload.yaml"}, webEnvironment = RANDOM_PORT)
public class UploadJourneyTests extends AbstractBasePageTest {

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "uploadFlow/docUploadJourney";
    super.setUp();
  }

  @Test
  void documentUploadFlow() {
    Assertions.assertThat(testPage.getTitle()).isEqualTo("Upload Documents");
    // Test HEIC and TIFF/TIF files still throw an error even when they are in the list of accepted file types
    uploadFile("another-test-heic.heic", "uploadTest");
    Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> testPage.findElementsByClass("text--error").get(0).getText()
        .contains("We are unable to process TIFF files. Please convert your file to a JPG or PNG and try again."));
    testPage.clickLink("remove");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");
    uploadFile("test.tif", "uploadTest");
    Assertions.assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo("We are unable to process TIFF files. Please convert your file to a JPG or PNG and try again.");
    testPage.clickLink("remove");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");
    // Test accepted file types
    // Extension list comes from application.yaml -- form-flow.uploads.accepted-file-types
    uploadFile("test-platypus.gif", "uploadTest");
    Assertions.assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(
            "We aren't able to upload this type of file. Please try another file that ends in one of the following: .jpeg, .fake, .heic, .tif, .tiff");
    testPage.clickLink("remove");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");
    // Upload a file that is too big and assert the correct error shows - max file size in test is 1MB
    long largeFilesize = 21000000L;
    driver.executeScript(
        "$('#document-upload-uploadTest').get(0).dropzone.addFile({name: 'testFile.pdf', size: "
            + largeFilesize + ", type: 'not-an-image'})");
    int maxFileSize = 17;
    Assertions.assertThat(driver.findElement(By.className("text--error")).getText()).contains(
        "This file is too large and cannot be uploaded (max size: " + maxFileSize + " MB)");
    testPage.clickLink("remove");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");
    uploadJpgFile("uploadTest");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("1 file added");
    uploadJpgFile("uploadTest"); // 2
    uploadJpgFile("uploadTest"); // 3
    uploadJpgFile("uploadTest"); // 4
    uploadJpgFile("uploadTest"); // 5
    uploadJpgFile("uploadTest"); // Can't upload the 6th
    Assertions.assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(
            "You have uploaded the maximum number of files. You will have the opportunity to share more with a caseworker later.");
    testPage.clickLink("remove");
    // Assert there are no longer any error after removing the errored item
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("5 files added");
    Assertions.assertThat(
            testPage.findElementsByClass("text--error").stream().map(WebElement::getText).collect(Collectors.toList()))
        .allMatch(String::isEmpty);
    // Delete all elements on the page and then assert there are no longer any uploaded files on the page
    while (testPage.findElementsByClass("dz-remove").size() > 0) {
      testPage.clickLink("delete");
      driver.switchTo().alert().accept();
    }
    Assertions.assertThat(testPage.findElementsByClass("dz-remove").size()).isEqualTo(0);
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("0 files added");
  }
}
