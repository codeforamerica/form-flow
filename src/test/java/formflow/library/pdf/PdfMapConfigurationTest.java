package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfMapConfigurationTest {

  @Test
  void getPdfFromFlowReturnsPdfWithMatchingFlowName() {
    String testPdfFilename = "/pdfs/testPdf.pdf";
    PdfMapConfiguration config = new PdfMapConfiguration(List.of(
        new PdfMap("flow1", testPdfFilename, Map.of(), Map.of(), Map.of(), Map.of()),
        new PdfMap("flow2", "/pdfs/testFlow/Multipage-UBI-Form.pdf", Map.of(), Map.of(), Map.of(), Map.of())
    ));
    assertThat(config.getPdfPathFromFlow("flow1")).isEqualTo(testPdfFilename);
  }

  @Test
  void getPdfFromFlowThrowsExceptionIfConfigMatchingFlowDoesntExist() {
    PdfMapConfiguration config = new PdfMapConfiguration(List.of(
        new PdfMap("flow1", "pdf", Map.of(), Map.of(), Map.of(), Map.of())
    ));
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> config.getPdfPathFromFlow("flow2"));
  }
}
