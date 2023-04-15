package formflow.library.pdf;

import static org.mockito.Mockito.mock;
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
  void setUp() {
    pdfGenerator = new PdfGenerator(submissionRepositoryService, documentFieldPreparers, pdfFieldMapper);
    submission = Submission.builder().id(UUID.randomUUID()).inputData(
        Map.of("firstName", "Greatest", "lastName", "Ever")).build();
    testPdfName = "Multipage-UBI-Form.pdf";
    when(submissionRepositoryService.findById(submission.getId())).thenReturn(Optional.of(submission));
    DocumentField firstNameField = new DocumentField("APPLICANT_LEGAL_NAME_FIRST", "Greatest", DocumentFieldType.SINGLE_VALUE, null);
    DocumentField lastNameField = new DocumentField("APPLICANT_CITY", "Minneapolis", DocumentFieldType.SINGLE_VALUE, null);

    when(documentFieldPreparers.prepareDocumentFields(submission)).thenReturn(List.of(firstNameField, lastNameField));
    when(pdfFieldMapper.map(List.of(firstNameField), "ubi")).thenReturn(List.of(new PdfField("APPLICANT_LEGAL_NAME_FIRST", "Greatest")));
    when(pdfFieldMapper.map(List.of(lastNameField), "ubi")).thenReturn(List.of(new PdfField("APPLICANT_CITY", "Minneapolis")));
  }

  @Test
  void generateReturnsAFilledPdf() throws IOException {
    preparePdfForAssertions(
        pdfGenerator.generate(new ApplicationFile(getBytesFromTestPdf(testPdfName), testPdfName), submission.getId())
    );

    assertPdfFieldEquals("APPLICANT_LEGAL_NAME_FIRST", submission.getInputData().get("firstName").toString());
    assertPdfFieldEquals("APPLICANT_CITY", submission.getInputData().get("lastName").toString());
  }

}