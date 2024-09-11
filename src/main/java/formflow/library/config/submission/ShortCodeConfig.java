package formflow.library.config.submission;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ShortCodeConfig {

    @Value("${form-flow.short-code.length:6}")
    private int codeLength;
}
