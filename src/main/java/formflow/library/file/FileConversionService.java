package formflow.library.file;

import com.google.common.io.Files;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.jetbrains.annotations.NotNull;
import org.openpdf.text.Document;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfCopy;
import org.openpdf.text.pdf.PdfImportedPage;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileConversionService {

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
    private final Tika tikaFileValidator;
    @Value("${form-flow.uploads.max-file-size}")
    Integer maxFileSize;
    @Value("${form-flow.uploads.file-conversion.prefix:}")
    private String convertedPrefix;
    @Value("${form-flow.uploads.file-conversion.suffix:}")
    private String convertedSuffix;
    @Value("${form-flow.uploads.file-conversion.max-conversion-size:}")
    private Integer maxConversionSize;
    @Value("${form-flow.uploads.file-conversion.max-pages:}")
    private Integer maxPages;
    @Value("${form-flow.uploads.file-conversion.allow-pdf-modification:false}")
    private boolean allowPdfModification;

    public FileConversionService() {
        tikaFileValidator = new Tika();
    }

    private static @NotNull File getModifiedPDFFile(File tempFile) throws IOException {
        String modifiedPDFPath =
                Files.getNameWithoutExtension(Objects.requireNonNull(tempFile.getAbsolutePath())) + "-modified.pdf";

        PdfReader reader = null;
        Document document = null;
        FileOutputStream outputStream = null;
        PdfCopy copy = null;

        try {
            reader = new PdfReader(tempFile.getAbsolutePath());
            reader.setModificationAllowedWithoutOwnerPassword(true);

            outputStream = new FileOutputStream(modifiedPDFPath);
            document = new Document(reader.getPageSizeWithRotation(1));
            copy = new PdfCopy(document, outputStream);

            document.open();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                copy.addPage(copy.getImportedPage(reader, i));
            }

        } finally {
            if (document != null) {
                document.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }

        return new File(modifiedPDFPath);
    }

    public Set<MultipartFile> convertFileToPDF(MultipartFile file) {
        try {
            MimeType fileMimeType = MimeType.valueOf(tikaFileValidator.detect(file.getInputStream()));

            if (MIME_TYPE_MAP.get(fileMimeType) == null) {
                log.error("Unable to convert Mime Type to PDF {}", fileMimeType);
                return new HashSet<>();
            }

            return switch (MIME_TYPE_MAP.get(fileMimeType)) {
                case IMAGE -> {
                    log.info("Converting image of type {} to PDF", fileMimeType);
                    yield convertImageToPDF(file);
                }
                case OFFICE_DOCUMENT -> {
                    log.info("Converting document of type {} to PDF", fileMimeType);
                    yield convertOfficeDocumentToPDF(file, 0);
                }
                default -> {
                    if (allowPdfModification) {
                        log.info("Allowing pdf modification for file of type {}", fileMimeType);
                        yield relaxPDFSecuritySettings(file);
                    } else {
                        log.info("Skipping converting file of type {} to PDF", fileMimeType);
                        yield new HashSet<>();
                    }
                }
            };
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new HashSet<>();
        }
    }

    private Set<MultipartFile> convertImageToPDF(MultipartFile file) {
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
            MultipartFile convertedPDF = createMultipartFile(file, new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

            Set<MultipartFile> result = new HashSet<>();

            if (isTooLarge(convertedPDF)) {
                log.info("Converted PDF is too large. Converted image is {} bytes.", convertedPDF.getSize());

                for (float q = 0.80f; q >= 0.0f; q = q - 0.05f) {
                    float compressionQuality = Math.round(q * 100) / 100.0f;
                    log.info("Reducing image quality to {}", compressionQuality);

                    // Convert byte array output stream to MultipartFile
                    convertedPDF = createCompressedAndScaledImagePDF(file, compressionQuality);

                    log.info("Compressed file size {}", convertedPDF.getSize());

                    if (!isTooLarge(convertedPDF)) {
                        log.info("Successfully converted image to PDF with file size: {}", convertedPDF.getSize());
                        break;
                    }
                }
            }

            if (isTooLarge(convertedPDF)) {
                log.warn("Compressed image PDF is still too large.");
            }

            result.add(convertedPDF);
            return result;

        } catch (IOException e) {
            log.error("Unable to convert Image to PDF", e);
            throw new RuntimeException(e);
        }
    }

    private MultipartFile createCompressedAndScaledImagePDF(MultipartFile file, float compressionQuality) throws IOException {
        byte[] compressedImage = compressAndScaleImage(file.getBytes(), compressionQuality);

        Document document = new Document(PageSize.LETTER);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, byteArrayOutputStream);

        document.open();

        // Read the image from MultipartFile
        Image image = Image.getInstance(compressedImage);

        // Scale image to fit the PDF page
        image.scaleToFit(PageSize.LETTER.getWidth() - 50, PageSize.LETTER.getHeight() - 50);
        image.setAlignment(Image.ALIGN_CENTER);

        // Add image to PDF
        document.add(image);

        document.close();

        // Convert byte array output stream to MultipartFile
        return createMultipartFile(file, new ByteArrayInputStream(byteArrayOutputStream.toByteArray()),
                "compression-" + compressionQuality);
    }


    private byte[] compressAndScaleImage(byte[] image, float quality) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(image);
        BufferedImage originalImage = ImageIO.read(byteArrayInputStream);

        // Resize image
        BufferedImage resizedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, originalImage.getWidth(), originalImage.getHeight(), null);
        g2d.dispose();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(byteArrayOutputStream);
        writer.setOutput(ios);

        // Set compression parameters
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        // Write compressed image
        writer.write(null, new IIOImage(resizedImage, null, null), param);
        ios.close();
        writer.dispose();

        return byteArrayOutputStream.toByteArray();
    }

    private Set<MultipartFile> convertOfficeDocumentToPDF(MultipartFile file, int numberOfRetries) {
        File inputFile;
        File pdfFile;
        File compressedPDFFile;

        Set<File> tempFiles = new HashSet<>();

        try {
            // Write to a temp file, so we can have a File from the original MultipartFile
            // OfficeLibre aka soffice requires a file on disk, not in memory
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.contains("..") || originalFilename.contains("/")
                    || originalFilename.contains("\\")) {
                throw new IllegalArgumentException("Unable to convert Office Document to PDF. Invalid filename.");
            }

            File safeDir = new File(System.getProperty("java.io.tmpdir"));
            inputFile = File.createTempFile("upload_", "." + FilenameUtils.getExtension(originalFilename), safeDir);
            file.transferTo(inputFile);

            File outputDir = inputFile.getParentFile();
            String outputFileName = Files.getNameWithoutExtension(inputFile.getName());
            String outputPdfPath = outputDir.getAbsolutePath() + "/" + outputFileName + ".pdf";

            // Run the soffice command to convert to PDF
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "soffice",
                    "--headless",
                    "--convert-to", "pdf:writer_pdf_Export:ReduceImageResolution=true,MaxImageResolution=150,Quality=80",
                    "--outdir", outputDir.getAbsolutePath(),
                    inputFile.getAbsolutePath()
            );

            tempFiles.add(inputFile);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // Read the converted PDF from disk
            pdfFile = new File(outputPdfPath);
            if (!pdfFile.exists()) {
                if (numberOfRetries < 10) {
                    numberOfRetries++;
                    log.warn("Unable to find PDF file {} with exit code: {}, will retry with attempt {}",
                            pdfFile.getAbsolutePath(), exitCode, numberOfRetries);
                    return convertOfficeDocumentToPDF(file, numberOfRetries);
                } else {
                    throw new RuntimeException(
                            "PDF conversion failed. Unable to find PDF file " + pdfFile.getAbsolutePath() + " after  "
                                    + numberOfRetries + " retries. Final exit code: " + exitCode);
                }
            }

            Set<MultipartFile> result = new HashSet<>();

            String convertedPDFPath;
            if (isTooLarge(pdfFile)) {
                // If the converted PDF is too large, we can use OpenPDF to further compressed it. This isn't possible
                // with LibreOffice, so it's another step and only needs to be done if the conversion increased the file
                // size to an extreme
                log.info("Converted PDF is too large. Converted file is {} bytes.", pdfFile.length());
                convertedPDFPath = pdfFile.getAbsolutePath() + "-compressed";
                PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());
                FileOutputStream outputStream = new FileOutputStream(convertedPDFPath);
                PdfStamper stamper = new PdfStamper(reader, outputStream, PdfWriter.VERSION_1_7);

                // Enable full compression
                stamper.getWriter().setFullCompression();
                stamper.getWriter().setCompressionLevel(9);  // Max compression

                // Remove unused objects
                reader.removeUnusedObjects();

                stamper.close();
                reader.close();
                outputStream.close();

                compressedPDFFile = new File(convertedPDFPath);
                log.info("Compressed PDF is {} bytes.", compressedPDFFile.length());

                if (isTooLarge(compressedPDFFile)) {
                    log.info("Compressed PDF is still too large. Trying to divide into multiple files.");

                    reader = new PdfReader(compressedPDFFile.getAbsolutePath());
                    int totalPages = reader.getNumberOfPages();

                    if (maxPages != null && totalPages > maxPages) {
                        log.warn("Too many pages found for PDF conversion. Only converting {} of {} pages.", maxPages,
                                totalPages);
                        totalPages = maxPages;
                    }

                    if (totalPages > 1) {
                        for (int i = 1; i <= totalPages; i++) {
                            String pageNumber = String.format("%02d", i); // Pads single digits with a 0, so 7 --> 07
                            String outputFilePath = compressedPDFFile.getAbsolutePath() + "_page_" + pageNumber + ".pdf";
                            Document document = new Document();
                            PdfCopy writer = new PdfCopy(document, new FileOutputStream(outputFilePath));
                            document.open();

                            PdfImportedPage page = writer.getImportedPage(reader, i);
                            writer.addPage(page);

                            document.close();
                            writer.close();

                            File pdfPageFile = new File(outputFilePath);
                            MultipartFile convertedPDF = createMultipartFile(file, pdfPageFile, "page_" + pageNumber);

                            if (isTooLarge(convertedPDF)) {
                                // The compressed pdf page is too big, but there's nothing else to do, so we can save and upload
                                // Clients should probably set up an alert based on this WARN!
                                log.warn("Converted PDF page {} is too large at {} bytes", i, convertedPDF.getSize());
                            }

                            tempFiles.add(pdfPageFile);
                            result.add(convertedPDF);
                        }
                    } else {
                        // The compressed pdf is too big, but it's only 1 page so it's good enough to save and upload
                        // Clients should probably set up an alert based on this WARN!
                        log.warn("Compressed PDF is still too large and only 1 page.");
                        MultipartFile convertedPDF = createMultipartFile(file, compressedPDFFile);
                        result.add(convertedPDF);
                    }

                } else {
                    // The compressed pdf is good to save and upload
                    MultipartFile convertedPDF = createMultipartFile(file, compressedPDFFile);
                    result.add(convertedPDF);
                }

                tempFiles.add(compressedPDFFile);

            } else {
                // The original converted PDF is good to save and upload
                MultipartFile convertedPDF = createMultipartFile(file, pdfFile);
                result.add(convertedPDF);
            }

            tempFiles.add(pdfFile);

            return result;
        } catch (IOException | InterruptedException e) {
            log.error("Unable to convert Office Document to PDF", e);
            throw new RuntimeException(e);
        } finally {
            // Clean up temporary files
            for (File tempFile : tempFiles) {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
    }

    /**
     * Takes a multipartfile representation of a PDF and makes a clone of it, with the relax security settings A read-only PDF is
     * now not as read-only
     */
    private Set<MultipartFile> relaxPDFSecuritySettings(MultipartFile file) {
        String originalFilename = file.getOriginalFilename() != null ? Files.getNameWithoutExtension(file.getOriginalFilename())
                : UUID.randomUUID().toString();

        // Remove illegal filename characters \ / : * ? " < > | and .
        String cleanFilename = originalFilename.replaceAll("[\\\\/:*?\"<>|.]", "");

        // Write to a temp file, so we can have a File from the original MultipartFile
        File tempFile = null;
        try {
            // Use cleanFilename, not originalFilename, for the temp file to reduce user input risk.
            // Additionally, limit its length and fallback to UUID if too short, ensuring no path traversal.
            String safeFilename =
                    (!cleanFilename.isEmpty() && cleanFilename.length() < 50) ? cleanFilename : UUID.randomUUID().toString();
            tempFile = File.createTempFile("upload_" + safeFilename + "_", ".tmp");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            File modifiedPDFFile = getModifiedPDFFile(tempFile);
            String convertedFileName = cleanFilename + "-modified.pdf";
            MultipartFile newFile = new MockMultipartFile(convertedFileName, convertedFileName, "application/pdf",
                    new FileInputStream(modifiedPDFFile));
            return Set.of(newFile);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private MultipartFile createMultipartFile(MultipartFile file, InputStream pdf, String suffix) throws IOException {
        String convertedFileName = convertFileName(file.getOriginalFilename(), suffix);
        return new MockMultipartFile(convertedFileName, convertedFileName, "application/pdf",
                pdf);
    }

    private MultipartFile createMultipartFile(MultipartFile file, InputStream pdf) throws IOException {
        return createMultipartFile(file, pdf, null);
    }

    private MultipartFile createMultipartFile(MultipartFile file, File pdf, String suffix) throws IOException {
        return createMultipartFile(file, new FileInputStream(pdf), suffix);
    }

    private MultipartFile createMultipartFile(MultipartFile file, File pdf) throws IOException {
        return createMultipartFile(file, new FileInputStream(pdf), null);
    }

    private String convertFileName(String originalFilename, String suffix) {
        String fileExtension = Files.getFileExtension(originalFilename);
        fileExtension = !fileExtension.isEmpty() ? "-" + fileExtension.toLowerCase() : "";
        fileExtension = suffix != null ? fileExtension + "-" + suffix : fileExtension;

        return convertedPrefix + Files.getNameWithoutExtension(Objects.requireNonNull(originalFilename)) + fileExtension
                + convertedSuffix + ".pdf";
    }

    private boolean isTooLarge(MultipartFile file) {
        return file.getSize() > (getMaxConversionSize() * FileValidationService.MB_IN_BYTES);
    }

    private boolean isTooLarge(File file) {
        return file.length() > (getMaxConversionSize() * FileValidationService.MB_IN_BYTES);
    }

    private Integer getMaxConversionSize() {
        if (maxConversionSize == null) {
            maxConversionSize = maxFileSize;
        }
        return maxConversionSize;
    }

    private enum CONVERSION_TO_PDF_TYPE {
        IMAGE, OFFICE_DOCUMENT, NONE;
    }
}
