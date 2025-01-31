package formflow.library.file;

import com.google.common.io.Files;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileConversionService {

    @Value("${form-flow.uploads.file-conversion.prefix:}")
    private String convertedPrefix;

    @Value("${form-flow.uploads.file-conversion.suffix:}")
    private String convertedSuffix;

    private final Map<MimeType, CONVERSION_TO_PDF_TYPE> MIME_TYPE_MAP = Map.ofEntries(
        Map.entry(MediaType.IMAGE_GIF, CONVERSION_TO_PDF_TYPE.IMAGE),
        Map.entry(MediaType.IMAGE_PNG, CONVERSION_TO_PDF_TYPE.IMAGE),
        Map.entry(MediaType.IMAGE_JPEG, CONVERSION_TO_PDF_TYPE.IMAGE),
        Map.entry(new MimeType("image", "bmp"), CONVERSION_TO_PDF_TYPE.IMAGE),
        Map.entry(new MimeType("application", "pdf"), CONVERSION_TO_PDF_TYPE.NONE),
        Map.entry(new MimeType("application", "msword"), CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT),
        Map.entry(new MimeType("application", "x-tika-msoffice"), CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT),
        Map.entry(new MimeType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document"),
                CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT),
        Map.entry(new MimeType("application", "x-tika-ooxml"), CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT),
        Map.entry(new MimeType("application", "vnd.oasis.opendocument.presentation"), CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT),
        Map.entry(new MimeType("application", "vnd.oasis.opendocument.spreadsheet"), CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT),
        Map.entry(new MimeType("application", "vnd.oasis.opendocument.text"), CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT),
        Map.entry(new MimeType("application", "zip"), CONVERSION_TO_PDF_TYPE.OFFICE_DOCUMENT)
    );

    private enum CONVERSION_TO_PDF_TYPE {
        IMAGE, OFFICE_DOCUMENT, NONE;
    }

    private final Tika tikaFileValidator;

    public FileConversionService() {
        tikaFileValidator = new Tika();
    }

    public MultipartFile convertFileToPDF(MultipartFile file) {
        try {
            MimeType fileMimeType = MimeType.valueOf(tikaFileValidator.detect(file.getInputStream()));

            if (MIME_TYPE_MAP.get(fileMimeType) == null) {
                log.error("Unable to convert Mime Type to PDF {}", fileMimeType);
                return null;
            }

            return switch (MIME_TYPE_MAP.get(fileMimeType)) {
                case IMAGE -> {
                    log.info("Converting image of type {} to PDF", fileMimeType);
                    yield convertImageToPDF(file);
                }
                case OFFICE_DOCUMENT -> {
                    log.info("Converting document of type {} to PDF", fileMimeType);
                    yield convertOfficeDocumentToPDF(file);
                }
                default -> {
                    log.info("Skipping converting file of type {} to PDF", fileMimeType);
                    yield null;
                }
            };
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private MultipartFile convertImageToPDF(MultipartFile file) {
        try {
            // Create a PDF document
            Document document = new Document(PageSize.LETTER);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, byteArrayOutputStream);

            document.open();

            // Read the image from MultipartFile
            Image image = Image.getInstance(file.getBytes());

            // Scale image to fit the PDF page
            image.scaleToFit(PageSize.LETTER.getWidth() - 50, PageSize.LETTER.getHeight() - 50);
            image.setAlignment(Image.ALIGN_CENTER);

            // Add image to PDF
            document.add(image);

            document.close();

            // Convert byte array output stream to MultipartFile
            return new MockMultipartFile("file", convertFileName(file.getOriginalFilename()), "application/pdf",
                    new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            log.error("Unable to convert Image to PDF", e);
            throw new RuntimeException(e);
        }
    }

    private MultipartFile convertOfficeDocumentToPDF(MultipartFile file) {
        try {
            // Write to a temp file, so we can have a File from the original MultipartFile
            // OfficeLibre aka soffice requires a file on disk, not in memory
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
                throw new IllegalArgumentException("Unable to convert Office Document to PDF. Invalid filename.");
            }

            File safeDir = new File(System.getProperty("java.io.tmpdir"));
            File inputFile = File.createTempFile("upload_", "." + FilenameUtils.getExtension(originalFilename), safeDir);
            file.transferTo(inputFile);

            File outputDir = inputFile.getParentFile();
            String outputFileName = Files.getNameWithoutExtension(inputFile.getName());
            String outputPdfPath = outputDir.getAbsolutePath() + "/" + outputFileName + ".pdf";

            // Run the soffice command to convert to PDF
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "soffice",
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", outputDir.getAbsolutePath(),
                    inputFile.getAbsolutePath()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            process.waitFor();

            // Read the converted PDF from disk
            File pdfFile = new File(outputPdfPath);
            if (!pdfFile.exists()) {
                throw new FileNotFoundException("PDF conversion failed, output file not found: " + outputPdfPath);
            }

            // Convert PDF file on disk into a stream and create a new MultipartFile
            MultipartFile pdfMultipartFile = new MockMultipartFile("file", convertFileName(file.getOriginalFilename()), "application/pdf",
                    new FileInputStream(pdfFile));

            // Clean up temporary files
            inputFile.delete();
            pdfFile.delete();

            return pdfMultipartFile;
        } catch (IOException | InterruptedException e) {
            log.error("Unable to convert Office Document to PDF", e);
            throw new RuntimeException(e);
        }
    }

    private String convertFileName(String originalFilename) {
        String fileExtension = Files.getFileExtension(originalFilename);
        fileExtension = !fileExtension.isEmpty() ? "-" + fileExtension.toLowerCase() : "";

        return convertedPrefix + Files.getNameWithoutExtension(Objects.requireNonNull(originalFilename)) + fileExtension  + convertedSuffix + ".pdf";
    }
}
