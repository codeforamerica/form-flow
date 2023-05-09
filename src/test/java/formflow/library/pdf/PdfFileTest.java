package formflow.library.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.boot.system.ApplicationTemp;

class PdfFileTest {
    @Test
    void copyToTempFileCopiesFileToTempDirectory() throws IOException {
        String testPdfPath = "/pdfs/testPdf.pdf";
        InputStream unfilledTestPdf = PdfFile.class.getResourceAsStream(testPdfPath);
        PdfFile pdfFile = new PdfFile(testPdfPath, "testPdf.pdf");
        PdfFile tempFile = PdfFile.copyToTempFile(pdfFile);

        assertThat(tempFile.path()).contains(new ApplicationTemp().getDir().getAbsolutePath(), "/testPdf.pdf");
        assertThat(tempFile.fileBytes()).isEqualTo(unfilledTestPdf.readAllBytes());
    }

    @Test
    void finalizeForSendingRemovesFieldsToLockEditingAndPreserveViewCompatibility() throws IOException {
        PdfFile pdfFile = new PdfFile(PdfFile.class.getResource("/pdfs/testPdf.pdf").getPath(), "testPdf.pdf");
        byte[] originalBytes = pdfFile.fileBytes();
        pdfFile.finalizeForSending();

        assertThat(PDDocument.load(pdfFile.fileBytes()).getDocumentCatalog().getAcroForm().getFields()).isEmpty();
        assertThat(originalBytes.length).isGreaterThan(pdfFile.fileBytes().length);
    }
}