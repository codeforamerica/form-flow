package formflow.library.pdf;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfGeneratorTest extends PdfTest {

  private PdfGenerator pdfGenerator;
  private Submission submission;
  private final SubmissionRepositoryService submissionRepositoryService = mock(SubmissionRepositoryService.class);
  private final SubmissionFieldPreparers submissionFieldPreparers = mock(SubmissionFieldPreparers.class);
  private final PdfFieldMapper pdfFieldMapper = mock(PdfFieldMapper.class);
  private String radioButtonValue;
  private String textFieldValue;
  private String checkboxOptionValue;

  @BeforeEach
  void setUp() throws IOException {
    PdfMapConfiguration pdfMapConfiguration = spy(new PdfMapConfiguration(List.of()));
    pdfGenerator = new PdfGenerator(submissionRepositoryService, submissionFieldPreparers, pdfFieldMapper, pdfMapConfiguration);
    submission = Submission.builder().id(UUID.randomUUID()).build();
    String testPdfName = "testPdf.pdf";
    textFieldValue = "Greatest Text";
    radioButtonValue = "option2";
    checkboxOptionValue = "Yes";
    ApplicationFile emptyPdf = new ApplicationFile(getBytesFromTestPdf(testPdfName), testPdfName);
    List<SubmissionField> singleFields = List.of(
        new SingleField("textField", textFieldValue, null),
        new SingleField("radioButton", radioButtonValue, null),
        new CheckboxField("checkbox", List.of("CheckboxOption1", "CheckboxOption3"), null)
    );

    doReturn(emptyPdf).when(pdfMapConfiguration).getPdfFromFlow("ubi");
    when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
    when(submissionFieldPreparers.prepareSubmissionFields(submission)).thenReturn(singleFields);
    when(pdfFieldMapper.map(singleFields, "ubi")).thenReturn(List.of(
        new PdfField("TEXT_FIELD", textFieldValue),
        new PdfField("RADIO_BUTTON", radioButtonValue),
        new PdfField("CHECKBOX_OPTION_1", checkboxOptionValue),
        new PdfField("CHECKBOX_OPTION_3", checkboxOptionValue)
    ));
  }

  @Test
  void generateReturnsAFilledPdfForEveryFieldType() throws IOException {
    preparePdfForAssertions(
        pdfGenerator.generate("ubi", submission.getId())
    );

    assertPdfFieldEquals("TEXT_FIELD", textFieldValue);
    assertPdfFieldEquals("RADIO_BUTTON", radioButtonValue);
    assertPdfFieldEquals("CHECKBOX_OPTION_1", checkboxOptionValue);
    assertPdfFieldEquals("CHECKBOX_OPTION_2", "Off");
    assertPdfFieldEquals("CHECKBOX_OPTION_3", checkboxOptionValue);
  }

}