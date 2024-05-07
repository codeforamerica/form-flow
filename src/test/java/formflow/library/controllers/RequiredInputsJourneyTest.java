package formflow.library.controllers;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.time.Duration;
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
    wait = new WebDriverWait(driver, Duration.ofSeconds(3));  // setting up a wait of 3 seconds
  }

  @Test
  public void automaticallyAppendsRequiredToAppropriateFields() {
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for='textInput']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for='areaInput']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for='numberInput']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for='selectInput']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for='moneyInput']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for='phoneInput']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("label[for='ssnInput']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("legend[for='date']"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("checkboxSet-legend"), "(required)"));
    wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("radioInput-legend"), "(required)"));
  }
}
