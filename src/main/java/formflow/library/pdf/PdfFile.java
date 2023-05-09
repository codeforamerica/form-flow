package formflow.library.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.boot.system.ApplicationTemp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public record PdfFile(String path, String name) {

    public static PdfFile copyToTempFile(String path) {
        InputStream unfilledPdf = PdfFile.class.getResourceAsStream(path);
        int fileExtension = path.length() - 4;
        File tempFile;
        Path pathToTempFile;
        try {
            tempFile = File.createTempFile(path, path.substring(fileExtension), new ApplicationTemp().getDir());
            pathToTempFile = Files.write(tempFile.toPath(), unfilledPdf.readAllBytes());
        } catch (NullPointerException | IOException e) {
            throw new RuntimeException("Could not copy pdf resource to temp file: " + e);
        }
        return new PdfFile(pathToTempFile.toAbsolutePath().toString(), getNameFromPath(path));
    }

    private static String getNameFromPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
    }

    @Override
    public String toString() {
        return path;
    }

    public byte[] fileBytes() throws IOException {
        return Files.readAllBytes(Paths.get(this.path));
    }

    public void finalizeForSending() throws IOException {
        PDDocument pdDocument = PDDocument.load(fileBytes());
        pdDocument.getDocumentCatalog().getAcroForm().flatten();
        pdDocument.save(path);
        pdDocument.close();
    }
}
