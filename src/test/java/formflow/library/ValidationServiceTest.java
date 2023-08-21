package formflow.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import formflow.library.config.ActionManager;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationServiceTest {

  private Validator validator;
  private final ActionManager actionManager = mock(ActionManager.class);
  private Submission submission;
  ValidationService validationService;

  @BeforeEach
  void setUp() {
    submission = Submission.builder()
        .inputData(Map.of(
                "firstName", "Name Here"
            )
        )
        .urlParams(new HashMap<>())
        .flow("testFlow")
        .build();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
    validationService = new ValidationService(validator, actionManager, "formflow.library.inputs.");
  }

  @Test
  void validateReturnsErrorsIfFound() {
    FormSubmission formSubmission = new FormSubmission(Map.ofEntries(Map.entry("dateFull", "")));
    assertThat(validationService.validate(new ScreenNavigationConfiguration(), "testFlow", formSubmission, submission)
        .get("dateFull")).containsExactlyInAnyOrder("Date must be in the format of mm/dd/yyyy", "Date may not be empty");
  }

  @Test
  void validateReturnsEmptyIfValidationsPass() {
    FormSubmission formSubmission = new FormSubmission(Map.ofEntries(Map.entry("firstName", "Guy Bourgeois")));
    assertThat(validationService.validate(new ScreenNavigationConfiguration(), "testFlow", formSubmission, submission)).isEqualTo(
        Map.of()
    );
  }
}
