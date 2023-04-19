package formflow.library.pdf;

import org.jetbrains.annotations.NotNull;

public record SingleField(String name, @NotNull String value, SubmissionFieldValue type, Integer iteration) implements
    SubmissionField {
}
