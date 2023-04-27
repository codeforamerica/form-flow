package formflow.library.pdf;

import org.jetbrains.annotations.NotNull;

public record SingleField(String name, @NotNull String value, Integer iteration) implements
    SubmissionField {
}
