package formflow.library.pdf;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PdfMapConfiguration {

    private final List<PdfMap> maps;

    List<PdfMap> getMaps() {
        return maps;
    }

    public PdfMapConfiguration(List<PdfMap> maps) {
        this.maps = maps;
    }

    public PdfFile getPdfFromFlow(String flow) {
        PdfMap pdfConfig = getPdfMap(flow);
        String pdfPath = pdfConfig.getPdf().startsWith("/") ? pdfConfig.getPdf() : "/" + pdfConfig.getPdf();
        return new PdfFile(pdfPath, pdfConfig.getPdf());
    }

    public PdfMap getPdfMap(String flow) {
        return maps.stream().filter(config -> config.getFlow().equals(flow))
                .findFirst().orElseThrow(RuntimeException::new);
    }
}
