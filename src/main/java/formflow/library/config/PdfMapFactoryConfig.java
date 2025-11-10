package formflow.library.config;

import formflow.library.pdf.PdfMap;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Factory for PdfMapConfiguration configuration.
 */
@Configuration
@ConditionalOnProperty(name = "form-flow.pdf.map-file")
public class PdfMapFactoryConfig {

    /**
     * Default constructor.
     */
    public PdfMapFactoryConfig() {
    }

    @Bean
    public PdfMapFactory pdfMapFactory() {
        return new PdfMapFactory();
    }

    /**
     * Bean to get a list of FlowConfiguration objects.
     *
     * @return list of flow configuration objects
     */
    @Bean
    public List<PdfMap> pdfMaps() {
        return pdfMapFactory().getObject();
    }
}
