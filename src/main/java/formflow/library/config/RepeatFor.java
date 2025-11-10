package formflow.library.config;

import lombok.Data;

@Data
public class RepeatFor {
    /**
     * Default constructor.
     */
    public RepeatFor() {
    }

    private String inputName;
    private String saveDataAs;
}