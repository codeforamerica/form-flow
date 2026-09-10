package formflow.library.pdf;

import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Built-in {@link DefaultSubmissionFieldPreparer} that maps a checkbox-set input field to a PDF field: for every
 * map-valued entry in a {@code pdf-map.yaml} file's field configuration, looks up the matching {@code field + "[]"}
 * list of selected values in the submission's input data and prepares it as a {@link CheckboxField}.
 */
@Component
public class OneToManyPreparer implements DefaultSubmissionFieldPreparer {

    /**
     * Default constructor.
     */
    public OneToManyPreparer() {
    }

    @Override
    public Map<String, SubmissionField> prepareSubmissionFields(Submission submission, PdfMap pdfMap) {
        Map<String, Object> fieldMap = pdfMap.getAllFields();
        Map<String, SubmissionField> preppedFields = new HashMap<>();

        fieldMap.keySet().stream()
                .filter(field -> fieldMap.get(field) instanceof Map && submission.getInputData().get(field + "[]") != null)
                .forEach(field ->
                        preppedFields.put(field, new CheckboxField(
                                        field,
                                        ((List<?>) submission.getInputData().get(field + "[]")).stream()
                                                .map(String.class::cast)
                                                .toList(),
                                        null
                                )
                        )
                );
        return preppedFields;
    }
}
