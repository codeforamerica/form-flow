package formflow.library.pdf;

import java.util.List;

public record CheckboxField(String name, List<String> value, SubmissionFieldValue type, Integer iteration) implements SubmissionField {
}
