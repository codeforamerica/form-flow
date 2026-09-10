package formflow.library.pdf;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Provides access to every flow's {@link PdfMap}, as configured across the application's {@code pdf-map.yaml}
 * file(s).
 */
@Component
public class PdfMapConfiguration {

    private final List<PdfMap> maps;

    /**
     * Creates a configuration wrapping every flow's PDF field mapping.
     *
     * @param maps every flow's PDF field mapping configuration
     */
    public PdfMapConfiguration(List<PdfMap> maps) {
        this.maps = maps;
    }

    List<PdfMap> getMaps() {
        return maps;
    }

    /**
     * Based on a specific form flow, get the PDF file path (and name) associated with that flow.
     *
     * @param flow The form flow to get the PDF file path for
     * @return path to PDF file (including filename), prefixed with '/'
     */
    public String getPdfPathFromFlow(String flow) {
        String pdf = getPdfMap(flow).getPdf();
        return pdf.startsWith("/") ? pdf : "/" + pdf;
    }

    /**
     * Based on a specific form flow, get the PdfMap associated with that flow.
     *
     * @param flow The form flow to get the PdfMap for
     * @return PdfMap object for the form flow
     */
    public PdfMap getPdfMap(String flow) {
        return maps.stream().filter(config -> config.getFlow().equals(flow))
                .findFirst().orElseThrow(RuntimeException::new);
    }
}
