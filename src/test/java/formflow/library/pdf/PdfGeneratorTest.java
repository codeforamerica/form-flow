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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class PdfGeneratorTest {

  private PdfGenerator pdfGenerator;
  private Submission submission;
  private final SubmissionRepositoryService submissionRepositoryService = mock(SubmissionRepositoryService.class);
  private final SubmissionFieldPreparers submissionFieldPreparers = mock(SubmissionFieldPreparers.class);
  private final PdfFieldMapper pdfFieldMapper = mock(PdfFieldMapper.class);
  private String radioButtonValue;
  private String textFieldValue;
  private String checkboxOptionValue;
  private PDFBoxFieldFiller pdfBoxFieldFiller = mock(PDFBoxFieldFiller.class);
  private PDDocument filledPdf;
  private String testPdfName;

  @BeforeEach
  void setUp() throws IOException {
    PdfMapConfiguration pdfMapConfiguration = spy(new PdfMapConfiguration(List.of()));
    pdfGenerator = new PdfGenerator(submissionRepositoryService, submissionFieldPreparers, pdfFieldMapper, pdfMapConfiguration, pdfBoxFieldFiller);
    submission = Submission.builder().id(UUID.randomUUID()).build();
    testPdfName = "testPdf.pdf";
    textFieldValue = "Greatest Text";
    radioButtonValue = "option2";
    checkboxOptionValue = "Yes";
    PdfFile emptyPdf = new PdfFile("/pdfs/" + testPdfName, testPdfName);
    List<SubmissionField> submissionFields = List.of(
        new SingleField("textField", textFieldValue, null),
        new SingleField("radioButton", radioButtonValue, null),
        new CheckboxField("checkbox", List.of("CheckboxOption1", "CheckboxOption3"), null)
    );

    doReturn(emptyPdf).when(pdfMapConfiguration).getPdfFromFlow("ubi");
    when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
    when(submissionFieldPreparers.prepareSubmissionFields(submission)).thenReturn(submissionFields);
    List<PdfField> pdfFields = List.of(
        new PdfField("TEXT_FIELD", textFieldValue),
        new PdfField("RADIO_BUTTON", radioButtonValue),
        new PdfField("CHECKBOX_OPTION_1", checkboxOptionValue),
        new PdfField("CHECKBOX_OPTION_3", checkboxOptionValue)
    );
    when(pdfFieldMapper.map(submissionFields, "ubi")).thenReturn(pdfFields);
    filledPdf = PDDocument.load(emptyPdf.fileBytes());
    when(pdfBoxFieldFiller.fill(List.of(new ByteArrayResource(emptyPdf.fileBytes())), pdfFields, emptyPdf.fileName())).thenReturn(
        filledPdf);
  }

  @AfterEach
  void tearDown() throws IOException {
    filledPdf.close();
  }

  @Test
  void generateReturnsAFileFilledByPdfBox() throws IOException {
    PdfFile pdf = pdfGenerator.generate("ubi", submission.getId());
    assertThat(pdf).isEqualTo(new PdfFile(filledPdf, testPdfName));
  }
}