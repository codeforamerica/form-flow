package formflow.library.framework;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.utilities.AbstractMockMvcTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-inputs.yaml"})
@DirtiesContext()
public class InputsTest extends AbstractMockMvcTest {

  @Test
  void shouldPersistInputValuesWhenNavigatingBetweenScreens() throws Exception {
    String textInput = "foo";
    String areaInput = "foo bar baz";
    String dateMonth = "10";
    String dateDay = "30";
    String dateYear = "2020";
    String numberInput = "123";
    // First "" value is from hidden input that a screen would submit
    List<String> checkboxSet = List.of("", "Checkbox-A", "Checkbox-B");
    List<String> checkboxInput = List.of("", "checkbox-value");
    String radioInput = "Radio B";
    String selectInput = "Select B";
    String moneyInput = "100";
    String phoneInput = "(555) 555-1234";
    String ssnInput = "333-22-4444";
    String stateInput = "NH";

    postExpectingNextPageTitle("inputs",
        Map.ofEntries(
            Map.entry("textInput", List.of(textInput)),
            Map.entry("areaInput", List.of(areaInput)),
            Map.entry("dateMonth", List.of(dateMonth)),
            Map.entry("dateDay", List.of(dateDay)),
            Map.entry("dateYear", List.of(dateYear)),
            Map.entry("numberInput", List.of(numberInput)),
            // CheckboxSet's need to have the [] in their name for POST actions
            Map.entry("checkboxSet[]", checkboxSet),
            // Checkboxes need to have the [] in their name for POST actions
            Map.entry("checkboxInput[]", checkboxInput),
            Map.entry("radioInput", List.of(radioInput)),
            Map.entry("selectInput", List.of(selectInput)),
            Map.entry("moneyInput", List.of(moneyInput)),
            Map.entry("phoneInput", List.of(phoneInput)),
            Map.entry("ssnInput", List.of(ssnInput)),
            Map.entry("stateInput", List.of(stateInput))),
        "Test");

    var inputsScreen = goBackTo("inputs");

    // Remove hidden value (our Screen Controller does this automatically)
    List<String> removedHiddenCheckboxSet = checkboxSet.stream().filter(e -> !e.isEmpty()).toList();
    List<String> removedHiddenCheckboxInput = checkboxInput.stream().filter(e -> !e.isEmpty()).toList();

    assertThat(inputsScreen.getInputValue("textInput")).isEqualTo(textInput);
    assertThat(inputsScreen.getTextAreaAreaValue("areaInput")).isEqualTo(areaInput);
    assertThat(inputsScreen.getInputValue("dateMonth")).isEqualTo(dateMonth);
    assertThat(inputsScreen.getInputValue("dateDay")).isEqualTo(dateDay);
    assertThat(inputsScreen.getInputValue("dateYear")).isEqualTo(dateYear);
    assertThat(inputsScreen.getInputValue("numberInput")).isEqualTo(numberInput);
    assertThat(inputsScreen.getCheckboxSetValues("checkboxSet")).isEqualTo(removedHiddenCheckboxSet);
    assertThat(inputsScreen.getCheckboxSetValues("checkboxInput")).isEqualTo(removedHiddenCheckboxInput);
    assertThat(inputsScreen.getRadioValue("radioInput")).isEqualTo(radioInput);
    assertThat(inputsScreen.getSelectValue("selectInput")).isEqualTo(selectInput);
    assertThat(inputsScreen.getInputValue("moneyInput")).isEqualTo(moneyInput);
    assertThat(inputsScreen.getInputValue("phoneInput")).isEqualTo(phoneInput);
    assertThat(inputsScreen.getInputValue("ssnInput")).isEqualTo(ssnInput);
    assertThat(inputsScreen.getSelectValue("stateInput")).isEqualTo(stateInput);
  }

  @Test
  void shouldOnlyRunValidationIfItHasARequiredAnnoation() throws Exception {
    // Should not validate when value is empty
    postExpectingNextPageTitle("pageWithOptionalValidation", "validatePositiveIfNotEmpty", "", "Success");
    // Should validate when a value is entered
    postExpectingFailureAndAssertErrorDisplaysForThatInput("pageWithOptionalValidation", "validatePositiveIfNotEmpty", "-2", "must be greater than 0");
    // Should redirect when input is valid
    postExpectingNextPageTitle("pageWithOptionalValidation", "validatePositiveIfNotEmpty", "2", "Success");
  }
}
