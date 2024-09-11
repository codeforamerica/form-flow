package formflow.library.config.submission;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ShortCodeConfig {

    public enum ShortCodeType {
        alphanumeric,
        alpha,
        numeric;
    }

    @Value("${form-flow.short-code.length:6}")
    private int codeLength;

    @Value("${form-flow.short-code.type:alphanumeric}")
    private ShortCodeType codeType;

    @Value("${form-flow.short-code.uppercase: true}")
    private boolean uppercase;

}
