package formflow.library.utils;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class AcceptedFileTypeUtils {

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

  @Value("${form-flow.uploads.accepted-file-types:''}")
  private String userProvidedFileTypes;

  public AcceptedFileTypeUtils() {

    List<String> userFileExts;
    List<String> serverFileExts = FILE_EXT_MIME_TYPE_MAP.keySet().stream().toList();

    if (userProvidedFileTypes != null && !userProvidedFileTypes.isBlank()) {
      userFileExts = stream(userProvidedFileTypes.replaceAll("\\s", "").split(",")).distinct().toList();
    } else {
      userFileExts = serverFileExts;
    }

    ACCEPTED_FILE_EXTS = userFileExts.stream()
        .filter(fileExt -> serverFileExts.contains(fileExt))
        .sorted()
        .toList();

    ACCEPTED_MIME_TYPES = ACCEPTED_FILE_EXTS.stream()
        .filter(value -> FILE_EXT_MIME_TYPE_MAP.containsKey(value))
        .map(value -> FILE_EXT_MIME_TYPE_MAP.get(value))
        .sorted()
        .toList();

    log.info(String.format("User provided file types: %s", userProvidedFileTypes));
    log.info(String.format("Files accepted by the server: %s", ACCEPTED_FILE_EXTS.stream().collect(Collectors.joining(","))));
  }

  public List<String> getAcceptableMimeTypes() {
    return ACCEPTED_MIME_TYPES;
  }

  public List<String> getAcceptableFileExts() {
    return ACCEPTED_FILE_EXTS;
  }

  /**
   * Returns True if the file is of the appropriate mime type for the server setup. This takes into account the configuration set
   * in the application's configuration.
   *
   * @param file the file to check the mime type of
   * @return Boolean True if the mimetype is one the system accepts, False otherwise.
   */
  public Boolean isAcceptedMimeType(MultipartFile file) {
    String contentType = file.getContentType();
    return ACCEPTED_MIME_TYPES.contains(contentType);
  }

  /**
   * Provides the list of acceptable file types in a string form, like so: ".bmp,.jpg"
   *
   * @return String a string containing a list of acceptable file extensions: ".bmp,.jpg"
   */
  public String acceptedFileTypes() {
    return ACCEPTED_FILE_EXTS.stream().collect(Collectors.joining(","));
  }
}
