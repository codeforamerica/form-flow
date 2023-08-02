package formflow.library.file;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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
  private final Map<String, MimeType> FILE_EXT_MIME_TYPE_MAP = Map.ofEntries(
      Map.entry(".gif", MediaType.IMAGE_GIF),
      Map.entry(".png", MediaType.IMAGE_PNG),
      Map.entry(".jpg", MediaType.IMAGE_JPEG),
      Map.entry(".jpeg", MediaType.IMAGE_JPEG),
      Map.entry(".bmp", new MimeType("image", "bmp")),
      Map.entry(".pdf", new MimeType("application", "pdf")),
      Map.entry(".doc", new MimeType("application", "msword")),
      Map.entry(".docx", new MimeType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document")),
      Map.entry(".odp", new MimeType("application", "vnd.oasis.opendocument.presentation")),
      Map.entry(".ods", new MimeType("application", "vnd.oasis.opendocument.spreadsheet")),
      Map.entry(".odt", new MimeType("application", "vnd.oasis.opendocument.text"))
  );

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

    ACCEPTED_MIME_TYPES = ACCEPTED_FILE_EXTS.stream()
        .filter(FILE_EXT_MIME_TYPE_MAP::containsKey)
        .map(FILE_EXT_MIME_TYPE_MAP::get)
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
  public Boolean isAcceptedMimeType(MultipartFile file) {
    if (file.getContentType() == null || file.getContentType().isBlank()) {
      return false;
    }
    return ACCEPTED_MIME_TYPES.contains(MimeType.valueOf(file.getContentType()));
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

  public Long getMaxFileSizeInMb() {
    return maxFileSize;
  }

}
