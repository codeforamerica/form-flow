package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InputDataToDocumentTypeConverterTest {

  private Submission testSubmission;

  @BeforeEach
  void setUp() {
    testSubmission = Submission.builder()
        .id(UUID.randomUUID())
        .submittedAt(DateTime.parse("2020-09-02").toDate())
        .flow("testFlow")
        .build();
  }

  @Test
  void submissionWithSingleInputShouldReturnCorrectValue() {
    String singleInputKey = "singleInput";
    Map<String, Object> inputData = Map.of(
        singleInputKey, "this is a test value");
    testSubmission.setInputData(inputData);
    assertThat(InputDataToDocumentFieldTypeConverter.getInputType(testSubmission, singleInputKey)).isEqualTo(
        FormInputType.SINGLE_VALUE);
  }

  @Test
  void inputKeyWithMultipleValuesShouldReturnAnArray() {
    String multipleInputKey = "multiValueInput";
    var multivalueArray = List.of("test1", "test2");
    Map<String, Object> inputData = Map.of(multipleInputKey, multivalueArray);
    testSubmission.setInputData(inputData);
    assertThat(InputDataToDocumentFieldTypeConverter.getInputType(testSubmission, multipleInputKey)).isEqualTo(
        FormInputType.MULTIVALUE_INPUT);
  }

}
