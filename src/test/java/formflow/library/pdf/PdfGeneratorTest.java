package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
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

class PdfGeneratorTest {

  private PdfGenerator pdfGenerator;
  private Submission submission;
  private final SubmissionRepositoryService submissionRepositoryService = mock(SubmissionRepositoryService.class);
  private final SubmissionFieldPreparers submissionFieldPreparers = mock(SubmissionFieldPreparers.class);
  private final PdfFieldMapper pdfFieldMapper = mock(PdfFieldMapper.class);
  private final PDFBoxFieldFiller pdfBoxFieldFiller = mock(PDFBoxFieldFiller.class);
  private String testPdfPath;

  @BeforeEach
  void setUp() throws IOException {
    PdfMapConfiguration pdfMapConfiguration = spy(new PdfMapConfiguration(List.of()));
    pdfGenerator = new PdfGenerator(submissionRepositoryService, submissionFieldPreparers, pdfFieldMapper, pdfMapConfiguration, pdfBoxFieldFiller);
    submission = Submission.builder().id(UUID.randomUUID()).build();
    testPdfPath = "/pdfs/testPdf.pdf";
    String textFieldValue = "Greatest Text";
    String radioButtonValue = "option2";
    String checkboxOptionValue = "Yes";
    PdfFile testPdf = mock(PdfFile.class);
    when(testPdf.fileBytes()).thenReturn(new byte[10]);
    when(testPdf.path()).thenReturn(testPdfPath);

    List<SubmissionField> submissionFields = List.of(
        new SingleField("textField", textFieldValue, null),
        new SingleField("radioButton", radioButtonValue, null),
        new CheckboxField("checkbox", List.of("CheckboxOption1", "CheckboxOption3"), null)
    );

    doReturn(testPdf).when(pdfMapConfiguration).getPdfFromFlow("ubi");
    when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
    when(submissionFieldPreparers.prepareSubmissionFields(submission)).thenReturn(submissionFields);
    List<PdfField> pdfFields = List.of(
        new PdfField("TEXT_FIELD", textFieldValue),
        new PdfField("RADIO_BUTTON", radioButtonValue),
        new PdfField("CHECKBOX_OPTION_1", checkboxOptionValue),
        new PdfField("CHECKBOX_OPTION_3", checkboxOptionValue)
    );
    when(pdfFieldMapper.map(submissionFields, "ubi")).thenReturn(pdfFields);
    PdfFile filledPdf = new PdfFile(testPdfPath, "testPdf.pdf");
    when(pdfBoxFieldFiller.fill(testPdf, pdfFields)).thenReturn(
        filledPdf);
  }
  @Test
  void generateReturnsAFileFilledByPdfBox() {
    PdfFile pdf = pdfGenerator.generate("ubi", submission.getId());
    assertThat(pdf).isEqualTo(new PdfFile(testPdfPath, "testPdf.pdf"));
  }
}