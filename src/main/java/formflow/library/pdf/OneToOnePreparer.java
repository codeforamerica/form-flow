package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Built-in {@link DefaultSubmissionFieldPreparer} that maps a single input field directly to a single PDF field:
 * for every string-valued entry in a {@code pdf-map.yaml} file's field configuration, looks up the matching value
 * in the submission's input data and prepares it as a {@link SingleField}.
 */
@Component
public class OneToOnePreparer implements DefaultSubmissionFieldPreparer {

    /**
     * Default constructor.
     */
    public OneToOnePreparer() {
    }

    @Override
    public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap) {
        Map<String, SubmissionField> preppedFields = new HashMap<>();
        Map<String, Object> fieldMap = pdfMap.getAllFields();

        fieldMap.keySet().stream()
                .filter(field -> (fieldMap.get(field) instanceof String) && (submission.getInputData().get(field) != null))
                .forEach(field ->
                        preppedFields.put(field, new SingleField(
                                        field,
                                        submission.getInputData().get(field).toString(),
                                        null
                                )
                        )
                );

        return preppedFields;
    }
}
