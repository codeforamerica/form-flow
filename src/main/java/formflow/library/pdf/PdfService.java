package formflow.library.pdf;

import formflow.library.data.Submission;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates a filled-in, flattened PDF for a submission by preparing its fields, mapping them onto a PDF
 * template's own field names, and filling the template.
 */
@Service
@Slf4j
public class PdfService {

    private final SubmissionFieldPreparers submissionFieldPreparers;
    private final PdfFieldMapper pdfFieldMapper;
    private final PdfMapConfiguration pdfMapConfiguration;
    private final PDFFormFiller pdfFormFiller;

    /**
     * Creates a PDF service backed by the given collaborators.
     *
     * @param submissionFieldPreparers prepares a submission's fields for PDF generation
     * @param pdfFieldMapper           maps prepared fields onto a PDF template's own field names
     * @param pdfMapConfiguration      provides each flow's PDF field mapping configuration
     * @param pdfFormFiller            fills a PDF template with the mapped field values
     */
    public PdfService(SubmissionFieldPreparers submissionFieldPreparers, PdfFieldMapper pdfFieldMapper,
            PdfMapConfiguration pdfMapConfiguration, PDFFormFiller pdfFormFiller) {
        this.submissionFieldPreparers = submissionFieldPreparers;
        this.pdfFieldMapper = pdfFieldMapper;
        this.pdfMapConfiguration = pdfMapConfiguration;
        this.pdfFormFiller = pdfFormFiller;
    }

    /**
     * Uses pdfGenerator to generate a pdf for a flow using a UUID.
     *
     * @param submission the submission for which the PDF should be generated
     * @return a pdf byte array
     * @throws IOException if an I/O error occurs while reading or writing the PDF file
     */
    public byte[] getFilledOutPDF(Submission submission) throws IOException {
        File file = generate(submission);
        byte[] pdfByteArray = Files.readAllBytes(file.toPath());
        file.delete();
        return pdfByteArray;
    }

    /**
     * Generates a PDF File based on Submission data
     *
     * @param submission the submission we are going to map the data of
     * @return A File which contains the path to the newly created and filled in PDF file.
     */
    public File generate(Submission submission) {
        List<SubmissionField> submissionFields = submissionFieldPreparers.prepareSubmissionFields(submission);
        List<PdfField> pdfFields = pdfFieldMapper.map(submissionFields, submission.getFlow());
        String pathToPdfResource = pdfMapConfiguration.getPdfPathFromFlow(submission.getFlow());
        return pdfFormFiller.fill(pathToPdfResource, pdfFields);
    }

    /**
     * Generates a generic pdf file name from the flow and submission id that are part of the Submission.
     *
     * @param submission Submission to create the PDF filename for
     * @return a generic filename string, including the '.pdf' extension
     */
    public String generatePdfName(Submission submission) {
        return String.format("%s_%s.pdf", submission.getFlow(), submission.getId());
    }
}
