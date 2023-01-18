package formflow.library.upload;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
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

    // Test accepted file types
    // Extension list comes from application.yaml -- form-flow.uploads.accepted-file-types
    uploadFile("test.tif", "doc-upload-files");
    Assertions.assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(
            "We aren't able to upload this type of file. Please try another file that ends in one of the following: .jpeg, .fake");
    testPage.clickLink("remove");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-doc-upload-files")).isEqualTo("0 files added");
    // Upload a file that is too big and assert the correct error shows - max file size in test is 1MB
    long largeFilesize = 21000000L;
    driver.executeScript(
        "$('#document-upload-doc-upload-files').get(0).dropzone.addFile({name: 'testFile.pdf', size: "
            + largeFilesize + ", type: 'not-an-image'})");
    int maxFileSize = 17;
    Assertions.assertThat(driver.findElement(By.className("text--error")).getText()).contains(
        "This file is too large and cannot be uploaded (max size: " + maxFileSize + " MB)");
    testPage.clickLink("remove");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-doc-upload-files")).isEqualTo("0 files added");
    uploadJpgFile("doc-upload-files");
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-doc-upload-files")).isEqualTo("1 file added");
    uploadJpgFile("doc-upload-files"); // 2
    uploadJpgFile("doc-upload-files"); // 3
    uploadJpgFile("doc-upload-files"); // 4
    uploadJpgFile("doc-upload-files"); // 5
    uploadJpgFile("doc-upload-files"); // Can't upload the 6th
    Assertions.assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(
            "You have uploaded the maximum number of files. You will have the opportunity to share more with a caseworker later.");
    testPage.clickLink("remove");
    // Assert there are no longer any error after removing the errored item
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-doc-upload-files")).isEqualTo("5 files added");
    Assertions.assertThat(
            testPage.findElementsByClass("text--error").stream().map(WebElement::getText).collect(Collectors.toList()))
        .allMatch(String::isEmpty);
    // Delete all elements on the page and then assert there are no longer any uploaded files on the page
    while (testPage.findElementsByClass("dz-remove").size() > 0) {
      testPage.clickLink("delete");
      driver.switchTo().alert().accept();
    }
    Assertions.assertThat(testPage.findElementsByClass("dz-remove").size()).isEqualTo(0);
    Assertions.assertThat(testPage.findElementTextById("number-of-uploaded-files-doc-upload-files")).isEqualTo("0 files added");
  }
}
