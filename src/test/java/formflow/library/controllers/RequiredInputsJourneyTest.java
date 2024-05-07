package formflow.library.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-required-inputs-flow.yaml"},
        webEnvironment = RANDOM_PORT)
public class RequiredInputsJourneyTest extends AbstractBasePageTest {

  private WebDriverWait wait;

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    String testFlow = "requiredInputs";
    startingPage = "flow/" + testFlow + "/inputs";
    super.setUp();
  }

  @Test
  public void automaticallyAppendsRequiredToAppropriateFields() {
    await().until(
            () -> !driver.findElements(By.className("main-footer")).get(0).getAttribute("innerHTML")
                    .isBlank());

    List<String> inputs = List.of(
            "textInput",
            "areaInput",
            "selectInput",
            "moneyInput",
            "phoneInput",
            "ssnInput");
    inputs.forEach(inputName -> {
      assertThat(driver.findElement(By.cssSelector(String.format("label[for='%s']", inputName))).getText()).contains("(required)");
    });
  }
}
