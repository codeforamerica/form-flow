package formflow.library.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import formflow.library.utilities.AbstractBasePageTest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"}, webEnvironment = RANDOM_PORT)
public class DisableMultipleFormSubmitTest extends AbstractBasePageTest {

    @Test
    void shouldNotSubmitFormMultipleTimes() {
        navigateTo("flow/testFlow/pageWithDefaultSubmitButton");
        WebElement formSubmitButton = testPage.findElementById("form-submit-button");
        WebElement form = driver.findElement(By.tagName("form"));
        driver.executeScript("arguments[0].addEventListener('submit', function(event) { event.preventDefault(); });", form);
        formSubmitButton.click();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5)); // wait up to 5 seconds
        wait.until(ExpectedConditions.attributeToBe(formSubmitButton, "disabled", "true"));
        assertThat(formSubmitButton.isEnabled()).isFalse();
    }

    @Test
    void testNoneCheckboxes() {
        navigateTo("flow/testFlow/pageWithCheckboxSetInput");

        testPage.enter("favoriteFruitCheckbox", List.of("Apple"));
        testPage.enter("favoriteVeggieCheckbox", List.of("Carrot"));

        assertThat(testPage.getCheckboxValues("favoriteFruitCheckbox")).containsExactly("Apple");
        assertThat(testPage.getCheckboxValues("favoriteVeggieCheckbox")).containsExactly("Carrot");

        testPage.enter("favoriteFruitCheckbox", List.of("Banana"));
        testPage.enter("favoriteVeggieCheckbox", List.of("None"));

        assertThat(testPage.getCheckboxValues("favoriteFruitCheckbox")).containsExactly("Apple", "Banana");
        assertThat(testPage.getCheckboxValues("favoriteVeggieCheckbox")).containsExactly("None");

        // Veggie "None" should be deselected
        testPage.enter("favoriteFruitCheckbox", List.of("None"));
        testPage.enter("favoriteVeggieCheckbox", List.of("Kale", "Carrot"));

        assertThat(testPage.getCheckboxValues("favoriteFruitCheckbox")).containsExactly("None");
        assertThat(testPage.getCheckboxValues("favoriteVeggieCheckbox")).containsExactly("Carrot", "Kale");
    }
}
