package formflow.library.pdf;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfGeneratorTest extends PdfTest {

  private PdfGenerator pdfGenerator;
  private Submission submission;
  private String testPdfName;
  private SubmissionRepositoryService submissionRepositoryService = mock(SubmissionRepositoryService.class);
  private DocumentFieldPreparers documentFieldPreparers = mock(DocumentFieldPreparers.class);
  private PdfFieldMapper pdfFieldMapper = mock(PdfFieldMapper.class);

  @BeforeEach
  void setUp() throws IOException {
    PdfMapConfiguration pdfMapConfiguration = spy(new PdfMapConfiguration(List.of()));
    pdfGenerator = new PdfGenerator(submissionRepositoryService, documentFieldPreparers, pdfFieldMapper, pdfMapConfiguration);
    submission = Submission.builder().id(UUID.randomUUID()).inputData(
        Map.of("firstName", "Greatest", "city", "Minneapolis")).build();
    testPdfName = "Multipage-UBI-Form.pdf";
    ApplicationFile emptyPdf = new ApplicationFile(getBytesFromTestPdf(testPdfName), testPdfName);
    List<DocumentField> documentFields = List.of(
        new DocumentField("APPLICANT_LEGAL_NAME_FIRST", "Greatest", DocumentFieldType.SINGLE_VALUE, null),
        new DocumentField("APPLICANT_CITY", "Minneapolis", DocumentFieldType.SINGLE_VALUE, null)
    );

    doReturn(emptyPdf).when(pdfMapConfiguration).getPdfFromFlow("ubi");
    when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
    when(documentFieldPreparers.prepareDocumentFields(submission)).thenReturn(documentFields);
    when(pdfFieldMapper.map(documentFields, "ubi")).thenReturn(List.of(
        new PdfField("APPLICANT_LEGAL_NAME_FIRST", "Greatest"),
        new PdfField("APPLICANT_CITY", "Minneapolis")
    ));
  }

  @Test
  void generateReturnsAFilledPdf() throws IOException {
    preparePdfForAssertions(
        pdfGenerator.generate("ubi", submission.getId())
    );

    assertPdfFieldEquals("APPLICANT_LEGAL_NAME_FIRST", submission.getInputData().get("firstName").toString());
    assertPdfFieldEquals("APPLICANT_CITY", submission.getInputData().get("city").toString());
  }

}