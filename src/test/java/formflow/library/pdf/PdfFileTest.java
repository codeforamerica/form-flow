package formflow.library.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.system.ApplicationTemp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PdfFileTest {
    PdfFile tempPdfFile;
    String testPdfPath = "/pdfs/testPdf.pdf";

    @BeforeEach
    void setUp() {
        tempPdfFile = PdfFile.copyToTempFile(testPdfPath);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.delete(Path.of(tempPdfFile.path()));
    }

    @Test
    void copyToTempFileCopiesFileToTempDirectory() throws IOException {
        InputStream unfilledTestPdf = PdfFile.class.getResourceAsStream(testPdfPath);

        assertThat(tempPdfFile.path()).contains(new ApplicationTemp().getDir().getAbsolutePath(), "/testPdf.pdf");
        assertThat(tempPdfFile.fileBytes()).isEqualTo(unfilledTestPdf.readAllBytes());
    }

    @Test
    void finalizeForSendingRemovesFieldsToLockEditingAndPreserveViewCompatibility() throws IOException {
        byte[] originalBytes = tempPdfFile.fileBytes();
        tempPdfFile.finalizeForSending();

        assertThat(PDDocument.load(tempPdfFile.fileBytes()).getDocumentCatalog().getAcroForm().getFields()).isEmpty();
        assertThat(originalBytes.length).isGreaterThan(tempPdfFile.fileBytes().length);
    }
}