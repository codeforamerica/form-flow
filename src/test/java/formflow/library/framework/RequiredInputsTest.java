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
import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-required-inputs-flow.yaml"})
@DirtiesContext
public class RequiredInputsTest extends AbstractMockMvcTest {
  
  @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }
  
  @Test
  void shouldIndicateRequiredFields() throws Exception {
    FormScreen inputsScreen = new FormScreen(mockMvc.perform(get("/flow/requiredInputs/inputs")));
    
    assertThat(inputsScreen.getElementByCssSelector("label[for=textInput]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementByCssSelector("label[for=areaInput]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementByCssSelector("legend[for=date]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementByCssSelector("label[for=numberInput]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementById("checkboxSet-legend").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementById("radioInput-legend").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementByCssSelector("label[for=selectInput]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementByCssSelector("label[for=moneyInput]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementByCssSelector("label[for=phoneInput]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
    assertThat(inputsScreen.getElementByCssSelector("label[for=ssnInput]").getElementsByClass("required-input").get(0).text())
        .contains("(required)");
  }
}
