package formflow.library.client;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"}, webEnvironment = RANDOM_PORT)
public class DisableMultipleFormSubmitTest extends AbstractBasePageTest {

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    startingPage = "flow/testFlow/pageWithDefaultSubmitButton";
    super.setUp();
  }
  
  @Test
  void shouldNotSubmitFormMultipleTimes() {
    WebElement formSubmitButton = testPage.findElementById("form-submit-button");
    WebElement form = driver.findElement(By.tagName("form"));
    driver.executeScript("arguments[0].addEventListener('submit', function(event) { event.preventDefault(); });", form);
    formSubmitButton.click();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5)); // wait up to 5 seconds
    wait.until(ExpectedConditions.attributeToBe(formSubmitButton, "disabled", "true"));
    assertThat(formSubmitButton.isEnabled()).isFalse();
  }
}
