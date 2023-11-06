package formflow.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import formflow.library.config.ActionManager;
import formflow.library.config.ScreenNavigationConfiguration;
import formflow.library.data.FormSubmission;
import formflow.library.data.Submission;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static formflow.library.inputs.FieldNameMarkers.DYNAMIC_FIELD_MARKER;

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

  @Test
  public void dynamicFieldShouldValidateCorrectly() {
    Map<String, Object> formSubmissionData = new HashMap<>();
    formSubmissionData.put("firstName", "OtherFirstName");
    IntStream.range(0, 5).forEach(n -> {
      formSubmissionData.put("dynamicField" + DYNAMIC_FIELD_MARKER + "123-" + n, "value " + n);
    });

    String badField = "dynamicField" + DYNAMIC_FIELD_MARKER + "123-5";
    formSubmissionData.put(badField, "");

    FormSubmission formSubmission = new FormSubmission(formSubmissionData);

    Map<String, List<String>> validationMessages = validationService.validate(new ScreenNavigationConfiguration(), "testFlow",
        formSubmission, submission);
    assertThat(validationMessages.size()).isEqualTo(1);
    assertThat(validationMessages.containsKey(badField)).isTrue();
    assertThat(validationMessages.get(badField).get(0)).isEqualTo("must not be blank");
  }

  @Test
  public void fieldIsDynamicFieldButDoesntHaveAnnotation() {
    Map<String, Object> formSubmissionData = new HashMap<>();
    String dynamicFieldName = "dynamicFieldNoAnnotation";
    formSubmissionData.put("firstName", "OtherFirstName");
    formSubmissionData.put(dynamicFieldName + DYNAMIC_FIELD_MARKER, "some value here");

    FormSubmission formSubmission = new FormSubmission(formSubmissionData);

    Throwable throwable = assertThrows(Throwable.class, () -> {
      validationService.validate(new ScreenNavigationConfiguration(), "testFlow", formSubmission, submission);
    });
    assertThat(throwable.getClass()).isEqualTo(RuntimeException.class);
    assertThat(throwable.getMessage()).isEqualTo(
        String.format(
            "Field name '%s' (field: '%s') acts like it's a dynamic field, but the field does not contain the @DynamicField annotation",
            dynamicFieldName, dynamicFieldName + DYNAMIC_FIELD_MARKER)
    );
  }

  @Test()
  public void fieldWithWildcardButIsntDynamicFieldShouldFailValidation() {
    Map<String, Object> formSubmissionData = new HashMap<>();
    String fieldName = "notDynamicField" + DYNAMIC_FIELD_MARKER;
    formSubmissionData.put("firstName", "OtherFirstName");
    formSubmissionData.put(fieldName, "some value here");

    FormSubmission formSubmission = new FormSubmission(formSubmissionData);

    Throwable throwable = assertThrows(Throwable.class, () -> {
      validationService.validate(new ScreenNavigationConfiguration(), "testFlow", formSubmission, submission);
    });
    assertThat(throwable.getClass()).isEqualTo(RuntimeException.class);
    assertThat(throwable.getMessage()).isEqualTo(
        String.format(
            "Input field '%s' has dynamic field marker '%s' in its name, but we are unable to find the field in the input file. Is it a dynamic field?",
            fieldName, DYNAMIC_FIELD_MARKER)
    );
  }
}
