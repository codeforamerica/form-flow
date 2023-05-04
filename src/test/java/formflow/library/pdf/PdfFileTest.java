package formflow.library.pdf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.system.ApplicationTemp;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PdfFileTest {
    @Test
    void constructingPdfFileWithPathToResourceCopiesToTmpDir() throws IOException {
        String filePath = "/pdfs/blankPdf.pdf";
        PdfFile pdfFile = new PdfFile(filePath, "blank.pdf");

        assertThat(pdfFile.fileName()).contains(new ApplicationTemp().getDir().getAbsolutePath(), "/blankPdf.pdf");
    }
}