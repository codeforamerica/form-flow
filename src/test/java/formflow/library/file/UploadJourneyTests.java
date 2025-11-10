package formflow.library.file;

import formflow.library.utilities.AbstractBasePageTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(properties = {
    "form-flow.path=flows-config/test-upload-flow.yaml",
    "form-flow.uploads.virus-scanning.enabled=false",
}, webEnvironment = RANDOM_PORT)
public class UploadJourneyTests extends AbstractBasePageTest {

  private final String dzWidgetName = "uploadTest";

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "flow/uploadFlowA/docUploadJourney";
    super.setUp();
  }

  @Test
  void documentUploadFlow() {
    assertThat(testPage.getTitle()).isEqualTo("Upload Documents");
    // Test accepted file types
    // Extension list comes from application.yaml -- form-flow.uploads.accepted-file-types
    uploadFile("test-platypus.gif", dzWidgetName);
    assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(messageSource
            .getMessage("upload-documents.error-invalid-file-type", null, Locale.ENGLISH)
            + " .jpeg, .pdf");
    testPage.clickLink("remove");
    assertThat(testPage.findElementTextById("number-of-uploaded-files-" + dzWidgetName)).isEqualTo("0 files added");

    // Upload a file that is too big and assert the correct error shows - max file size in test is 1MB
    long largeFilesize = 21000000L;
    driver.executeScript(
        "$('#document-upload-" + dzWidgetName + "').get(0).dropzone.addFile({name: 'testFile.pdf', size: "
            + largeFilesize + ", type: 'not-an-image'})");
    int maxFileSize = 17;
    assertThat(driver.findElement(By.className("text--error")).getText()).contains(messageSource
        .getMessage("upload-documents.this-file-is-too-large", new Object[]{maxFileSize}, Locale.ENGLISH));
    testPage.clickLink("remove");
    assertThat(testPage.findElementTextById("number-of-uploaded-files-" + dzWidgetName)).isEqualTo("0 files added");

    // Upload a password-protected file and assert the correct error shows
    uploadPasswordProtectedPdf(dzWidgetName);

    //Race condition caused by uploadPasswordProtectedPdf waits until upload file has file details added instead
    //of waiting until file upload is complete.
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(messageSource.getMessage("upload-documents.error-password-protected", null, Locale.ENGLISH));
    testPage.clickLink("remove");
    assertThat(testPage.findElementTextById("number-of-uploaded-files-" + dzWidgetName)).isEqualTo("0 files added");

    // Test max number of files that can be uploaded
    uploadJpgFile(dzWidgetName); // 1
    assertThat(testPage.findElementTextById("number-of-uploaded-files-uploadTest")).isEqualTo("1 file added");
    uploadJpgFile(dzWidgetName); // 2
    uploadJpgFile(dzWidgetName); // 3
    uploadJpgFile(dzWidgetName); // 4
    uploadJpgFile(dzWidgetName); // 5
    uploadJpgFile(dzWidgetName); // Can't upload the 6th
    assertThat(testPage.findElementsByClass("text--error").get(0).getText())
        .isEqualTo(messageSource.getMessage("upload-documents.error-maximum-number-of-files", null, Locale.ENGLISH));
    testPage.clickLink("remove");
    // Assert there are no longer any error after removing the errored item
    assertThat(testPage.findElementTextById("number-of-uploaded-files-" + dzWidgetName)).isEqualTo("5 files added");
    assertThat(
        testPage.findElementsByClass("text--error").stream().map(WebElement::getText).collect(Collectors.toList()))
        .allMatch(String::isEmpty);
    // Delete all elements on the page and then assert there are no longer any uploaded files on the page
    while (!testPage.findElementsByClass("dz-remove").isEmpty()) {
      testPage.clickLink("delete");
      driver.switchTo().alert().accept();
    }
    assertThat(testPage.findElementsByClass("dz-remove").size()).isEqualTo(0);
    assertThat(testPage.findElementTextById("number-of-uploaded-files-" + dzWidgetName)).isEqualTo("0 files added");
  }

  @Test
  void documentUploadFlowMultiFlow() {
    assertThat(testPage.getTitle()).isEqualTo("Upload Documents");

    for (int i = 0; i < 5; i++) {
      uploadFile("testA.jpeg", dzWidgetName);
    }

    assertThat(testPage.findElementTextById("number-of-uploaded-files-" + dzWidgetName)).isEqualTo("5 files added");
    assertThat(testPage.findElementsByClass("text--error").stream()
        .map(WebElement::getText)
        .collect(Collectors.toList()))
        .allMatch(String::isEmpty);

    List<WebElement> elementsA = testPage.findElementsByClass("filename-text-name");
    assertThat(elementsA.size()).isEqualTo(5);
    elementsA.forEach(
        element -> assertThat(element.getText()).isEqualTo("testA")
    );

    // switch flow from A to B and upload some files.  Ensure you only see Flow B files.
    baseUrl = "http://localhost:%s/%s".formatted(localServerPort, "flow/uploadFlowB/docUploadJourney");
    driver.navigate().to(baseUrl);

    for (int i = 0; i < 5; i++) {
      uploadFile("testB.jpeg", dzWidgetName);
    }

    assertThat(testPage.findElementTextById("number-of-uploaded-files-" + dzWidgetName)).isEqualTo("5 files added");
    assertThat(testPage.findElementsByClass("text--error").stream()
        .map(WebElement::getText)
        .collect(Collectors.toList()))
        .allMatch(String::isEmpty);

    List<WebElement> elementsB = testPage.findElementsByClass("filename-text-name");
    elementsB.forEach(
        element -> assertThat(element.getText()).isEqualTo("testB")
    );

    // go forward and back and ensure you only see flow B files
    testPage.clickContinue();
    testPage.goBack();
    // Wait for page to load after browser back navigation (may reload from bfcache)
    // First wait for title to be available, then wait for elements to ensure page is fully loaded
    await().until(() -> {
      try {
        String title = testPage.getTitle();
        return title.equals("Upload Documents");
      } catch (Exception e) {
        return false;
      }
    });
    // Now wait for the file elements to be available
    await().until(() -> {
      try {
        List<WebElement> elements = testPage.findElementsByClass("filename-text-name");
        return elements.size() == 5;
      } catch (Exception e) {
        return false;
      }
    });

    elementsB = testPage.findElementsByClass("filename-text-name");
    assertThat(elementsB.size()).isEqualTo(5);
    elementsB.forEach(
        element -> {
          assertThat(element.getText()).isEqualTo("testB");
        });

    // switch back to A and ensure no B flow files are present
    baseUrl = "http://localhost:%s/%s".formatted(localServerPort, "flow/uploadFlowA/docUploadJourney");
    driver.navigate().to(baseUrl);
    // Wait for page to load and elements to be available
    await().until(() -> {
      try {
        List<WebElement> elements = testPage.findElementsByClass("filename-text-name");
        return elements.size() == 5;
      } catch (Exception e) {
        return false;
      }
    });

    elementsA = testPage.findElementsByClass("filename-text-name");
    assertThat(elementsA.size()).isEqualTo(5);
    elementsA.forEach(
        element -> {
          assertThat(element.getText()).isEqualTo("testA");
        });
  }
}
