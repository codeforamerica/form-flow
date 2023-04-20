package formflow.library.pdf;

import org.jetbrains.annotations.NotNull;

public record DatabaseField(String name, @NotNull String value) implements SubmissionField {

}
