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
  private String city;
  private String firstName;

  @BeforeEach
  void setUp() throws IOException {
    PdfMapConfiguration pdfMapConfiguration = spy(new PdfMapConfiguration(List.of()));
    pdfGenerator = new PdfGenerator(submissionRepositoryService, submissionFieldPreparers, pdfFieldMapper, pdfMapConfiguration);
    submission = Submission.builder().id(UUID.randomUUID()).build();
    String testPdfName = "Multipage-UBI-Form.pdf";
    firstName = "Greatest";
    city = "Minneapolis";
    ApplicationFile emptyPdf = new ApplicationFile(getBytesFromTestPdf(testPdfName), testPdfName);
    List<SubmissionField> submissionFields = List.of(
        new SubmissionField("APPLICANT_LEGAL_NAME_FIRST", firstName, SubmissionFieldType.SINGLE_VALUE, null),
        new SubmissionField("APPLICANT_CITY", city, SubmissionFieldType.SINGLE_VALUE, null)
    );

    doReturn(emptyPdf).when(pdfMapConfiguration).getPdfFromFlow("ubi");
    when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
    when(submissionFieldPreparers.prepareSubmissionFields(submission)).thenReturn(submissionFields);
    when(pdfFieldMapper.map(submissionFields, "ubi")).thenReturn(List.of(
        new PdfField("APPLICANT_LEGAL_NAME_FIRST", firstName),
        new PdfField("APPLICANT_CITY", city)
    ));
  }

  @Test
  void generateReturnsAFilledPdf() throws IOException {
    preparePdfForAssertions(
        pdfGenerator.generate("ubi", submission.getId())
    );

    assertPdfFieldEquals("APPLICANT_LEGAL_NAME_FIRST", firstName);
    assertPdfFieldEquals("APPLICANT_CITY", city);
  }

}