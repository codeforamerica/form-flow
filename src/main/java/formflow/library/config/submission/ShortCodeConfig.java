package formflow.library.config.submission;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ShortCodeConfig {

    public enum ShortCodeType {
        alphanumeric, alpha, numeric;
    }

    public enum ShortCodeCreationPoint {
        creation, submission
    }

    @Value("${form-flow.short-code.length:6}")
    private int codeLength;

    @Value("${form-flow.short-code.type:alphanumeric}")
    private ShortCodeType codeType;

    @Value("${form-flow.short-code.uppercase: true}")
    private boolean uppercase;

    @Value("${form-flow.short-code.creation-point:submission}")
    private ShortCodeCreationPoint creationPoint;

    @Value("${form-flow.short-code.prefix:#{null}}")
    private String prefix;

    @Value("${form-flow.short-code.suffix:#{null}}")
    private String suffix;

    public boolean isCreateShortCodeAtCreation() {
        return ShortCodeCreationPoint.creation.equals(creationPoint);
    }

    public boolean isCreateShortCodeAtSubmission() {
        return ShortCodeCreationPoint.submission.equals(creationPoint);
    }
}
