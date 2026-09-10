package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.Map;

/**
 * Extension point for a consuming application to add its own PDF field preparation logic. {@link
 * SubmissionFieldPreparers} runs every bean of this type after the library's built-in {@link
 * DefaultSubmissionFieldPreparer} beans, so a custom preparer can override the fields the defaults produced for
 * the same field name.
 */
public interface SubmissionFieldPreparer {

    /**
     * Prepares the {@link SubmissionField}s this preparer is responsible for, keyed by field name.
     *
     * @param submission the submission to pull field values from
     * @param pdfMap     the PDF map configuration (from a {@code pdf-map.yaml} file) describing which fields to prepare
     * @return the prepared fields, keyed by field name
     */
    Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap);
}
