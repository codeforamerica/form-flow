package formflow.library.framework;

import static formflow.library.inputs.FieldNameMarkers.UNVALIDATED_FIELD_MARKER_VALIDATE_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import formflow.library.address_validation.AddressValidationService;
import formflow.library.address_validation.ValidatedAddress;
import formflow.library.utilities.AbstractMockMvcTest;
import formflow.library.utilities.FormScreen;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-required-inputs-flow.yaml"})
@DirtiesContext()
public class RequiredInputsTest extends AbstractMockMvcTest {

  @MockBean
  AddressValidationService addressValidationService;

  @Test
  void shouldIndicateRequiredFields() throws Exception {
    FormScreen inputsScreen = new FormScreen(mockMvc.perform(get("/flow/requiredInputs/inputs").session(session)));
    assertThat(inputsScreen.getElementByCssSelector("label[for=textInput]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("textInput").attr("aria-required"))
        .isEqualTo("true");
    assertThat(inputsScreen.getElementByCssSelector("label[for=areaInput]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("areaInput").attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementByCssSelector("legend[for=date]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("date-day").attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementByCssSelector("label[for=numberInput]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("numberInput").attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementById("checkboxSet-legend").text())
        .contains("*");
    assertThat(Objects.requireNonNull(inputsScreen.getElementById("checkboxSet-legend").parent()).attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementById("radioInput-legend").text())
        .contains("*");
    assertThat(Objects.requireNonNull(inputsScreen.getElementById("radioInput-legend").parent()).attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementByCssSelector("label[for=selectInput]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("selectInput").attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementByCssSelector("label[for=moneyInput]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("moneyInput").attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementByCssSelector("label[for=phoneInput]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("phoneInput").attr("aria-required")).isEqualTo("true");
    assertThat(inputsScreen.getElementByCssSelector("label[for=ssnInput]").text())
        .contains("*");
    assertThat(inputsScreen.getElementById("ssnInput").attr("aria-required")).isEqualTo("true");
  }
}
