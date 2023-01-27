package formflow.library.validation;

import formflow.library.utilities.AbstractBasePageTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(properties = {"form-flow.path=flows-config/test-validation.yaml"}, webEnvironment = RANDOM_PORT)
public class OptionalValidationJourneyTest extends AbstractBasePageTest {
    @Override
    @BeforeEach
    public void setUp() throws IOException {
        startingPage = "testFlow/pageWithOptionalValidation";
        super.setUp();
    }

    @Test
    void optionalValidationFlow() {
        Assertions.assertThat(testPage.getTitle()).isEqualTo("Optional validation page");
        // Click continue without entering any value and check that validation does not block
        testPage.clickContinue();
        Assertions.assertThat(testPage.getTitle()).isEqualTo("Last Page");

        // Go back, enter value that will trigger validation, and ensure that it works

        testPage.goBack();
        Assertions.assertThat(testPage.getTitle()).isEqualTo("Optional validation page");

        testPage.enter("validatePositiveIfNotEmpty", "-2");
        System.out.println(testPage.getInputValue("validatePositiveIfNotEmpty"));

        testPage.clickContinue();
        Assertions.assertThat(testPage.hasInputError("validatePositiveIfNotEmpty")).isTrue();
    }
}
