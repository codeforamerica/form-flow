package formflow.library.pdf;

import java.util.List;

public record CheckboxField(String name, List<String> value, Integer iteration) implements SubmissionField {

}
