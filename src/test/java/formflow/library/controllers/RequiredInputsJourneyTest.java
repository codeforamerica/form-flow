package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-required-inputs-flow.yaml"},
    webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public class RequiredInputsJourneyTest extends AbstractBasePageTest {


    @Override
  @BeforeEach
  public void setUp() throws IOException {
        String testFlow = "requiredInputs";
        startingPage = "flow/" + testFlow + "/inputs";
    super.setUp();
  }

  @Test
  public void automaticallyAppendsRequiredToAppropriateFields() {
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("label[for='textInput']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("label[for='areaInput']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("label[for='numberInput']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("label[for='selectInput']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("label[for='moneyInput']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("label[for='phoneInput']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("label[for='ssnInput']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.ByCssSelector.cssSelector("legend[for='date']")).getText()).contains("(required)");
    assertThat(driver.findElement(By.id("checkboxSet-legend")).getText()).contains("(required)");
    assertThat(driver.findElement(By.id("radioInput-legend")).getText()).contains("(required)");
  }
}
