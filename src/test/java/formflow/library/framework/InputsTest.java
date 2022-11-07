package formflow.library.framework;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.utilities.AbstractMockMvcTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-inputs.yaml"})
@DirtiesContext()
public class InputsTest extends AbstractMockMvcTest {

  @Override
  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Test
  void shouldPersistInputValuesWhenNavigatingBetweenScreens() throws Exception {
    String textInput = "foo";
    String areaInput = "foo bar baz";
    String dateMonth = "10";
    String dateDay = "30";
    String dateYear = "2020";
    String numberInput = "123";
    List<String> checkboxSet = List.of("Checkbox A", "Checkbox B");
    String checkboxInput = "checkbox value";
    String radioInput = "Radio B";
    String selectInput = "Select B";
    String moneyInput = "100";
    String phoneInput = "(555) 555-1234";
    String ssnInput = "333-22-4444";

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
            Map.entry("checkboxInput", List.of(checkboxInput)),
            Map.entry("radioInput", List.of(radioInput)),
            Map.entry("selectInput", List.of(selectInput)),
            Map.entry("moneyInput", List.of(moneyInput)),
            Map.entry("phoneInput", List.of(phoneInput)),
            Map.entry("ssnInput", List.of(ssnInput))),
        "Test");

    var inputsScreen = goBackTo("inputs");

    assertThat(inputsScreen.getInputValue("textInput")).isEqualTo(textInput);
    assertThat(inputsScreen.getTextAreaAreaValue("areaInput")).isEqualTo(areaInput);
    assertThat(inputsScreen.getInputValue("dateMonth")).isEqualTo(dateMonth);
    assertThat(inputsScreen.getInputValue("dateDay")).isEqualTo(dateDay);
    assertThat(inputsScreen.getInputValue("dateYear")).isEqualTo(dateYear);
    assertThat(inputsScreen.getInputValue("numberInput")).isEqualTo(numberInput);
    assertThat(inputsScreen.getCheckboxSetValues("checkboxSet")).isEqualTo(checkboxSet);
    assertThat(inputsScreen.getCheckboxValue("checkboxInput")).isEqualTo(checkboxInput);
    assertThat(inputsScreen.getRadioValue("radioInput")).isEqualTo(radioInput);
    assertThat(inputsScreen.getSelectValue("selectInput")).isEqualTo(selectInput);
    assertThat(inputsScreen.getInputValue("moneyInput")).isEqualTo(moneyInput);
    assertThat(inputsScreen.getInputValue("phoneInput")).isEqualTo(phoneInput);
    assertThat(inputsScreen.getInputValue("ssnInput")).isEqualTo(ssnInput);
  }
}
