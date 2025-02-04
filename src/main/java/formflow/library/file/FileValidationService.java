package formflow.library.file;

import static java.util.Arrays.stream;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

/**
 * This service is intended to help with miscellaneous file things. This service will help with checking mime types, both proper
 * mime-type names like "image/jpeg" and with file extensions. The current list of accepted file mime-types are:
 * <ul>
 *   <li> .bmp : image/bmp </li>
 *   <li> .doc : application/msword"</li>
 *   <li> .docx : application/vnd.openxmlformats-officedocument.wordprocessingml.document</li>
 *   <li> .gif : image/gif </li>
 *   <li> .jpeg : image/jpeg </li>
 *   <li> .jpg : image/jpeg </li>
 *   <li> .pdf : application/pdf </li>
 *   <li> .png : image/png </li>
 *   <li> .odp : application/vnd.oasis.opendocument.presentation </li>
 *   <li> .ods : application/vnd.oasis.opendocument.spreadsheet </li>
 *   <li> .odt : application/vnd.oasis.opendocument.text </li>
 * </ul>
 */
@Slf4j
@Service
public class FileValidationService {

  public static final long MB_IN_BYTES = 1024 * 1024;
  private final Map<String, Set<MimeType>> FILE_EXT_MIME_TYPE_MAP = Map.ofEntries(
      Map.entry(".gif", Set.of(MediaType.IMAGE_GIF)),
      Map.entry(".png", Set.of(MediaType.IMAGE_PNG)),
      Map.entry(".jpg", Set.of(MediaType.IMAGE_JPEG)),
      Map.entry(".jpeg", Set.of(MediaType.IMAGE_JPEG)),
      Map.entry(".bmp", Set.of(new MimeType("image", "bmp"))),
      Map.entry(".pdf", Set.of(new MimeType("application", "pdf"))),
      Map.entry(".doc", Set.of(new MimeType("application", "msword"), new MimeType("application", "x-tika-msoffice"))),
      Map.entry(".docx", Set.of(new MimeType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document"), new MimeType("application", "x-tika-ooxml"))),
      Map.entry(".odp", Set.of(new MimeType("application", "vnd.oasis.opendocument.presentation"))),
      Map.entry(".ods", Set.of(new MimeType("application", "vnd.oasis.opendocument.spreadsheet"))),
      Map.entry(".odt", Set.of(new MimeType("application", "vnd.oasis.opendocument.text")))
  );

  private final MimeType ZIP_MIME_TYPE = new MimeType("application", "zip");

  private final List<MimeType> ACCEPTED_MIME_TYPES;

  private final List<String> ACCEPTED_FILE_EXTS;

  private final String JOIN_DELIMITER = ", ";
  private final long maxFileSize;

  public FileValidationService(
      @Value("${form-flow.uploads.accepted-file-types:''}") String userProvidedFileTypes,
      @Value("${form-flow.uploads.max-file-size}") Integer maxFileSize
  ) {
    this.maxFileSize = maxFileSize;
    List<String> userFileExts;
    List<String> serverFileExts = FILE_EXT_MIME_TYPE_MAP.keySet().stream().toList();

    if (userProvidedFileTypes != null && !userProvidedFileTypes.isBlank()) {
      userFileExts = stream(userProvidedFileTypes.replaceAll("\\s", "").split(",")).distinct().toList();
    } else {
      userFileExts = serverFileExts;
    }

    ACCEPTED_FILE_EXTS = userFileExts.stream()
        .filter(serverFileExts::contains)
        .sorted()
        .toList();

    ACCEPTED_MIME_TYPES = FILE_EXT_MIME_TYPE_MAP.values().stream()
            .flatMap(Set::stream)
            .sorted()
            .toList();

    log.info(String.format("User provided file types: %s", userProvidedFileTypes));
    log.info(String.format("Files accepted by the server: %s", String.join(JOIN_DELIMITER, ACCEPTED_FILE_EXTS)));
  }

  /**
   * Returns the list of file extensions the system accepts: ".bmp, .doc"
   *
   * @return List of strings containing the acceptable file extensions for the system
   */
  public List<String> getAcceptableFileExts() {
    return ACCEPTED_FILE_EXTS;
  }

  /**
   * Returns True if the file is of the appropriate mime type for the system setup. This takes into account the configuration set
   * in the application's configuration.
   *
   * @param file the file to check the mime type of
   * @return Boolean True if the mimetype is one of the ones the system accepts, False otherwise.
   */
  public Boolean isAcceptedMimeType(MultipartFile file) throws IOException {
    Tika tikaFileValidator = new Tika();
    if (file.getContentType() == null || file.getContentType().isBlank()) {
      return false;
    }

    MimeType mimeType = MimeType.valueOf(tikaFileValidator.detect(file.getInputStream()));

    if (ZIP_MIME_TYPE.equals(mimeType) && ACCEPTED_FILE_EXTS.contains(".docx") &&
            Files.getFileExtension(Objects.requireNonNull(file.getOriginalFilename())).equalsIgnoreCase("docx")) {
      // docx files are technically just zip files with xml files inside of them. if the mime type is set to be a
      // zip file, and we accept docx files, we can check if the zip is actually a zip... or if it's a docx and return
      try (InputStream inputStream = file.getInputStream(); ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
          if (entry.getName().equals("word/document.xml") || entry.getName().equals("[Content_Types].xml")) {
            return true;
          }
        }
      }
    }

    return ACCEPTED_MIME_TYPES.contains(mimeType);
  }

  /**
   * Provides the list of acceptable file types the system accepts in a string form, like so: ".bmp,.jpg"
   *
   * @return String a string containing a list of acceptable file extensions: ".bmp,.jpg"
   */
  public String acceptedFileTypes() {
    return String.join(JOIN_DELIMITER, ACCEPTED_FILE_EXTS);
  }

  public boolean isTooLarge(MultipartFile file) {
    return file.getSize() > (maxFileSize * MB_IN_BYTES);
  }

  public boolean isTooLarge(File file) {
    return file.length() > (maxFileSize * MB_IN_BYTES);
  }

  public Long getMaxFileSizeInMb() {
    return maxFileSize;
  }

}
