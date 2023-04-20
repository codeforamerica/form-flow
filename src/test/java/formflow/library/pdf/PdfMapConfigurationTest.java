package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdfMapConfigurationTest {

  @Test
  void getPdfFromFlowReturnsPdfWithMatchingFlowName() throws IOException {
    String testPdfFilename = "/pdfs/testPdf.pdf";
    PdfMapConfiguration config = new PdfMapConfiguration(List.of(
        new PdfMap("flow1", testPdfFilename, Map.of(), Map.of()),
        new PdfMap("flow2", "/pdfs/Multipage-UBI-Form.pdf", Map.of(), Map.of())
    ));
    assertThat(config.getPdfFromFlow("flow1").fileName()).isEqualTo(testPdfFilename);
  }

  @Test
  void getPdfFromFlowThrowsExceptionIfConfigMatchingFlowDoesntExist() {
    PdfMapConfiguration config = new PdfMapConfiguration(List.of(
        new PdfMap("flow1", "pdf", Map.of(), Map.of())
    ));
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> config.getPdfFromFlow("flow2"));
  }
}