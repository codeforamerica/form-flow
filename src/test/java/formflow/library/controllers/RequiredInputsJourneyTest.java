package formflow.library.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-required-inputs-flow.yaml"},
    webEnvironment = RANDOM_PORT)
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
    assertThat(driver.findElement(By.cssSelector("label[for='textInput']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.cssSelector("label[for='areaInput']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.cssSelector("label[for='numberInput']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.cssSelector("label[for='selectInput']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.cssSelector("label[for='moneyInput']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.cssSelector("label[for='phoneInput']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.cssSelector("label[for='ssnInput']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.cssSelector("legend[for='date']")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.id("checkboxSet-legend")).findElement(By.className("required-input")).getText()).contains("(required)");
    assertThat(driver.findElement(By.id("radioInput-legend")).findElement(By.className("required-input")).getText()).contains("(required)");
  }

}
