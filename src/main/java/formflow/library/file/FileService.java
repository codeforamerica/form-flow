package formflow.library.file;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileService This service is intended to help with miscellaneous file things.
 * TODO: fix formatting
 * <p>
 * This service will help with checking mime types, both proper mime-type names like "image/jpeg" and with file extensions. The
 * current list of accepted file mime-types are: .bmp : image/bmp .doc : application/msword" .docx :
 * application/vnd.openxmlformats-officedocument.wordprocessingml.document .gif : image/gif .jpeg : image/jpeg .jpg : image/jpeg
 * .pdf : application/pdf .png : image/png .odp : application/vnd.oasis.opendocument.presentation .ods :
 * application/vnd.oasis.opendocument.spreadsheet .odt : application/vnd.oasis.opendocument.text
 * </p>
 */
@Slf4j
@Service
public class FileService {

  private final Map<String, String> FILE_EXT_MIME_TYPE_MAP = Map.ofEntries(
      Map.entry(".bmp", "image/bmp"),
      Map.entry(".doc", "application/msword"),
      Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
      Map.entry(".gif", "image/gif"),
      Map.entry(".jpeg", "image/jpeg"),
      Map.entry(".jpg", "image/jpeg"),
      Map.entry(".pdf", "application/pdf"),
      Map.entry(".png", "image/png"),
      Map.entry(".odp", "application/vnd.oasis.opendocument.presentation"),
      Map.entry(".ods", "application/vnd.oasis.opendocument.spreadsheet"),
      Map.entry(".odt", "application/vnd.oasis.opendocument.text")
  );

  private final List<String> ACCEPTED_MIME_TYPES;

  private final List<String> ACCEPTED_FILE_EXTS;

  private String userProvidedFileTypes;

  public FileService(
      @Value("${form-flow.uploads.accepted-file-types:''}") String userProvidedFileTypes) {
    this.userProvidedFileTypes = userProvidedFileTypes;
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
    log.info(String.format("Files accepted by the server: %s", String.join(",", ACCEPTED_FILE_EXTS)));
  }

  /**
   * Returns the list of mime types the system accepts, in the form of full mime type strings: ["image/bmp",
   * "application/msword"]
   *
   * @return List of strings containing acceptable mime types for the system
   */
  public List<String> getAcceptableMimeTypes() {
    return ACCEPTED_MIME_TYPES;
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
    String contentType = file.getContentType();
    return ACCEPTED_MIME_TYPES.contains(contentType);
  }

  /**
   * Provides the list of acceptable file types the system accepts in a string form, like so: ".bmp,.jpg"
   *
   * @return String a string containing a list of acceptable file extensions: ".bmp,.jpg"
   */
  public String acceptedFileTypes() {
    return ACCEPTED_FILE_EXTS.stream().collect(Collectors.joining(","));
  }
}
