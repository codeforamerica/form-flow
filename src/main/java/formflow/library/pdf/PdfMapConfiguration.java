package formflow.library.pdf;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PdfMapConfiguration {

    private final List<PdfMap> maps;

    List<PdfMap> getMaps() {
        return maps;
    }

    public PdfMapConfiguration(List<PdfMap> maps) {
        this.maps = maps;
    }

    public String getPdfFromFlow(String flow) {
        String pdf = getPdfMap(flow).getPdf();
        return pdf.startsWith("/") ? pdf : "/" + pdf;
    }

    public PdfMap getPdfMap(String flow) {
        return maps.stream().filter(config -> config.getFlow().equals(flow))
                .findFirst().orElseThrow(RuntimeException::new);
    }
}
