package formflow.library.file;

import com.google.common.io.Files;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileConversionService {

    public enum MIME_TYPE {
        GIF(MediaType.IMAGE_GIF, CONVERSION_TO_PDF_TYPE.IMAGE),
        PNG(MediaType.IMAGE_PNG, CONVERSION_TO_PDF_TYPE.IMAGE),
        JPG(MediaType.IMAGE_JPEG, CONVERSION_TO_PDF_TYPE.IMAGE),
        BMP(new MimeType("image", "bmp"), CONVERSION_TO_PDF_TYPE.IMAGE),
        PDF(new MimeType("application", "pdf"), CONVERSION_TO_PDF_TYPE.NONE),
        DOC(new MimeType("application", "msword"), CONVERSION_TO_PDF_TYPE.MS_DOCUMENT),
        DOCX(new MimeType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document"),
                CONVERSION_TO_PDF_TYPE.MS_DOCUMENT),
        ODP(new MimeType("application", "vnd.oasis.opendocument.presentation"), CONVERSION_TO_PDF_TYPE.MS_DOCUMENT),
        ODS(new MimeType("application", "vnd.oasis.opendocument.spreadsheet"), CONVERSION_TO_PDF_TYPE.MS_DOCUMENT),
        ODT(new MimeType("application", "vnd.oasis.opendocument.text"), CONVERSION_TO_PDF_TYPE.MS_DOCUMENT),
        TIKA_MS_DOC(new MimeType("application", "x-tika-msoffice"), CONVERSION_TO_PDF_TYPE.MS_DOCUMENT);

        private final MimeType mimeType;
        private final CONVERSION_TO_PDF_TYPE conversionType;

        MIME_TYPE(MimeType mimeType, CONVERSION_TO_PDF_TYPE conversionType) {
            this.mimeType = mimeType;
            this.conversionType = conversionType;
        }

        private static final Map<MimeType, MIME_TYPE> LOOKUP_MAP = new HashMap<>();

        static {
            for (MIME_TYPE type : MIME_TYPE.values()) {
                LOOKUP_MAP.put(type.mimeType, type);
            }
        }

        public static MIME_TYPE get(MimeType mimeType) {
            return LOOKUP_MAP.get(mimeType);
        }
    }

    public enum CONVERSION_TO_PDF_TYPE {
        IMAGE, MS_DOCUMENT, NONE;
    }

    private final Tika tikaFileValidator;

    public FileConversionService() {
        tikaFileValidator = new Tika();
    }

    public MultipartFile convertFileToPDF(MultipartFile file) {

        try {
            MimeType fileMimeType = MimeType.valueOf(tikaFileValidator.detect(file.getInputStream()));
            MIME_TYPE originalMimeType = MIME_TYPE.get(fileMimeType);

            if (originalMimeType == null) {
                log.error("Unable to convert Mime Type to PDF: {}", fileMimeType);
                throw new UnsupportedFileConversionMimeTypeException("Could not detect MIME type for file {}");
            }

            switch (originalMimeType.conversionType) {
                case IMAGE:
                    return convertImageToPDF(file);
                case MS_DOCUMENT:
                    break;
                case NONE:
                    break;
                default:
                    break;
            }


        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return file;
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

            String convertedFileName = Files.getNameWithoutExtension(Objects.requireNonNull(file.getOriginalFilename())) + "-converted.pdf";
            // Convert byte array output stream to MultipartFile
            return new MockMultipartFile("file", convertedFileName, "application/pdf",
                    new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
