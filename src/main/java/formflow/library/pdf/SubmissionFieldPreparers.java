package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orchestrates every {@link DefaultSubmissionFieldPreparer} and {@link SubmissionFieldPreparer} bean in the
 * application context to build the full set of {@link SubmissionField}s used to fill in a submission's PDF.
 */
@Slf4j
@Component
public class SubmissionFieldPreparers {

    private final List<DefaultSubmissionFieldPreparer> defaultPreparers;

    private final List<SubmissionFieldPreparer> customPreparers;

    private final PdfMapConfiguration pdfMapConfiguration;


    /**
     * Creates a preparer orchestrator backed by the given collaborators.
     *
     * @param defaultPreparers    the library's built-in field preparers, run first
     * @param customPreparers     a consuming application's custom field preparers, run after (and able to
     *                            override fields produced by) the default preparers
     * @param pdfMapConfiguration provides the {@link PdfMap} for a given flow
     */
    public SubmissionFieldPreparers(List<DefaultSubmissionFieldPreparer> defaultPreparers,
            List<SubmissionFieldPreparer> customPreparers,
            PdfMapConfiguration pdfMapConfiguration) {
        this.defaultPreparers = defaultPreparers;
        this.customPreparers = customPreparers;
        this.pdfMapConfiguration = pdfMapConfiguration;
    }

    /**
     * This method creates a list of SubmissionField objects which will be used to create the PdfField objects that will
     * eventually populate a given PDF. First it creates a single map of all input related data by flattening the subflow and
     * input data objects into a single map. Then, it iterates over each preparer and calls the prepareSubmissionFields passing
     * the submission, the flattened input data map and the PdfMap object from a given `pdf-map.yaml` file. First the default
     * preparers are called which will run the OneToOnePreparer, OneToManyPreparer and the DatabaseFieldPreparer. Then, it will
     * call any custom preparers that have been added to the application context by an implementer of the formflow library. The
     * custom preparers are called last so that any SubmissionField objects created by the default preparers can and will be
     * overridden by the custom preparers.
     *
     * @param submission the Submission that holds the data we need to prepare for the PDF file
     * @return a list of SubmissionField objects for all fields in a given PdfMap from a `pdf-map.yaml` file.
     */
    public List<SubmissionField> prepareSubmissionFields(Submission submission) {
        PdfMap pdfMap = pdfMapConfiguration.getPdfMap(submission.getFlow());
        HashMap<String, SubmissionField> submissionFieldsMap = new HashMap<>();

        // now run preparers over all the fields.
        defaultPreparers.forEach(preparer -> {
            try {
                submissionFieldsMap.putAll(preparer.prepareSubmissionFields(submission, pdfMap));
            } catch (Exception e) {
                String preparerClassName = preparer.getClass().getSimpleName();
                log.error("There was an issue preparing submission data for " + preparerClassName, e);
            }
        });

        customPreparers.forEach(preparer -> {
            try {
                submissionFieldsMap.putAll(preparer.prepareSubmissionFields(submission, pdfMap));
            } catch (Exception e) {
                String preparerClassName = preparer.getClass().getSimpleName();
                log.error("There was an issue preparing submission data for " + preparerClassName, e);
            }
        });

        return submissionFieldsMap.values().stream().toList();
    }
}
