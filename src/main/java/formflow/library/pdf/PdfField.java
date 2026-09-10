package formflow.library.pdf;

import java.util.Optional;

/**
 * A single field of an actual PDF form to fill in: the PDF's own field name (as configured in a {@code
 * pdf-map.yaml} file, not the submission's input field name) and the string value to write into it. Produced by
 * {@link PdfFieldMapper} from a {@link SubmissionField}.
 *
 * @param name  the PDF form's field name
 * @param value the value to fill the field with; a null value is stored as an empty string, since a PDF
 *              field can't be filled with null
 */
public record PdfField(String name, String value) {

    /**
     * Normalizes a null value to an empty string, since a PDF field can't be filled with null.
     *
     * @param name  the PDF form's field name
     * @param value the value to fill the field with
     */
    public PdfField(String name, String value) {
        this.name = name;
        this.value = Optional.ofNullable(value).orElse("");
    }

}
