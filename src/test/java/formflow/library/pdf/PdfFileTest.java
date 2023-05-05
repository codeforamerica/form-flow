package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.system.ApplicationTemp;

class PdfFileTest {
    String filePath;
    PdfFile pdfFile;
    @BeforeEach
    void setUp() {
        filePath = "/pdfs/testPdf.pdf";
        pdfFile = new PdfFile(filePath, "testPdf.pdf");
    }

    @Test
    void copyToTempFileCopiesFileToTempDirectory() throws IOException {
        PdfFile tempFile = PdfFile.copyToTempFile(pdfFile);
        assertThat(tempFile.path()).contains(new ApplicationTemp().getDir().getAbsolutePath(), "/testPdf.pdf");
        assertThat(tempFile.fileBytes()).isEqualTo(pdfFile.fileBytes());
    }

    @Test
    void finalizeForSendingRemovesFieldsToLockEditingAndPreserveViewCompatibility() throws IOException {
        pdfFile.finalizeForSending();
        assertThat(PDDocument.load(PdfFile.class.getResourceAsStream(pdfFile.path())).getDocumentCatalog().getAcroForm().getFields()).isEmpty();
    }
}