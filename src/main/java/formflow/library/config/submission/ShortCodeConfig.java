package formflow.library.config.submission;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the {@code form-flow.short-code} configuration properties: per-flow settings for generating a
 * human-readable, unique short code for a {@link formflow.library.data.Submission}.
 */
@Configuration
@ConfigurationProperties(prefix = "form-flow.short-code")
public class ShortCodeConfig {

    private Map<String, Config> shortCodeConfigs;

    /**
     * Default constructor.
     */
    public ShortCodeConfig() {
    }

    /**
     * Looks up the short code configuration for a given flow.
     *
     * @param flowName the flow to look up
     * @return the flow's short code configuration, or null if none is configured for that flow
     */
    public Config getConfig(String flowName) {
        return shortCodeConfigs != null ? shortCodeConfigs.get(flowName) : null;
    }

    /**
     * Sets the flow-name-to-short-code-configuration map.
     *
     * @param shortCodeConfigs the flow-name-to-short-code-configuration map, as bound from application properties
     */
    public void setShortCodeConfigs(Map<String, Config> shortCodeConfigs) {
        this.shortCodeConfigs = shortCodeConfigs;
    }

    /**
     * A single flow's short code generation settings: how long the code is, what characters it can contain,
     * whether it's uppercased, an optional prefix/suffix, and when in the flow the code gets generated.
     */
    @Setter
    @Getter
    public static class Config {

        private int codeLength = 6;
        private ShortCodeType codeType = ShortCodeType.alphanumeric;
        private boolean uppercase = true;
        private ShortCodeCreationPoint creationPoint = ShortCodeCreationPoint.submission;
        private String prefix = null;
        private String suffix = null;

        /**
         * Default constructor.
         */
        public Config() {
        }

        /**
         * Checks whether this flow's short code should be generated at submission creation time.
         *
         * @return true if this flow's short code should be generated when its submission is first created
         */
        public boolean isCreateShortCodeAtCreation() {
            return ShortCodeCreationPoint.creation.equals(creationPoint);
        }

        /**
         * Checks whether this flow's short code should be generated at submission time.
         *
         * @return true if this flow's short code should be generated when its submission is submitted
         */
        public boolean isCreateShortCodeAtSubmission() {
            return ShortCodeCreationPoint.submission.equals(creationPoint);
        }

        /**
         * The characters a generated short code may contain.
         */
        public enum ShortCodeType {
            /** Letters and digits. */
            alphanumeric,
            /** Letters only. */
            alpha,
            /** Digits only. */
            numeric
        }

        /**
         * When in a flow's lifecycle a short code gets generated.
         */
        public enum ShortCodeCreationPoint {
            /** Generate the short code as soon as the submission is created. */
            creation,
            /** Generate the short code when the submission is submitted. */
            submission
        }

    }
}
