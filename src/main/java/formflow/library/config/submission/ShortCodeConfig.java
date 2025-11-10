package formflow.library.config.submission;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "form-flow.short-code")
public class ShortCodeConfig {

    /**
     * Default constructor.
     */
    public ShortCodeConfig() {
    }

    private Map<String, Config> shortCodeConfigs;

    public Config getConfig(String flowName) {
        return shortCodeConfigs != null ? shortCodeConfigs.get(flowName) : null;
    }

    public void setShortCodeConfigs(Map<String, Config> shortCodeConfigs) {
        this.shortCodeConfigs = shortCodeConfigs;
    }

    @Setter
    @Getter
    public static class Config {

        /**
         * Default constructor.
         */
        public Config() {
        }

        public enum ShortCodeType {
            alphanumeric, alpha, numeric;
        }

        public enum ShortCodeCreationPoint {
            creation, submission
        }

        private int codeLength = 6;

        private ShortCodeType codeType = ShortCodeType.alphanumeric;

        private boolean uppercase = true;

        private ShortCodeCreationPoint creationPoint = ShortCodeCreationPoint.submission;

        private String prefix = null;

        private String suffix = null;

        public boolean isCreateShortCodeAtCreation() {
            return ShortCodeCreationPoint.creation.equals(creationPoint);
        }

        public boolean isCreateShortCodeAtSubmission() {
            return ShortCodeCreationPoint.submission.equals(creationPoint);
        }

    }
}
