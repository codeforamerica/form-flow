package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.Map;

/**
 * Marks a {@link SubmissionFieldPreparer}-shaped bean as one of the library's built-in preparers (currently
 * {@link OneToOnePreparer}, {@link OneToManyPreparer}, and {@link DatabaseFieldPreparer}). {@link
 * SubmissionFieldPreparers} runs every bean of this type before any {@link SubmissionFieldPreparer} beans, so a
 * consuming application's custom preparers can override fields the defaults produced.
 */
public interface DefaultSubmissionFieldPreparer {

    /**
     * Prepares the {@link SubmissionField}s this preparer is responsible for, keyed by field name.
     *
     * @param submission the submission to pull field values from
     * @param pdfMap     the PDF map configuration (from a {@code pdf-map.yaml} file) describing which fields to prepare
     * @return the prepared fields, keyed by field name
     */
    Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap);
}
