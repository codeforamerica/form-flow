package formflow.library.upload;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AcceptedFileTypeService {

  static List<String> ACCEPTED_FILE_TYPES = List.of(
      ".jpeg", ".jpg", ".png", ".pdf", ".bmp",
      ".gif", ".doc", ".docx", ".odt", ".ods", ".odp");

  static final List<String> ACCEPTED_MIME_TYPES = List.of("image/jpeg", "image/png", "application/pdf",
          "image/bmp", "application/pdf", "image/gif", "application/msword");
  private static String userProvidedFileTypes = "";

  public AcceptedFileTypeService(@Value("${form-flow.uploads.accepted-file-types:''}") String userProvidedFileTypes) {
    this.userProvidedFileTypes = userProvidedFileTypes;
  }

  public static Boolean isAcceptedMimeType(MultipartFile file){
    String contentType = file.getContentType();
    return ACCEPTED_MIME_TYPES.contains(contentType);
  }

  public static String acceptedFileTypes() {
    if (userProvidedFileTypes == null || userProvidedFileTypes.isEmpty()) {
      return String.join(",", ACCEPTED_FILE_TYPES);
    }

    return stream(userProvidedFileTypes.split(","))
        .distinct()
        .filter(fileType -> ACCEPTED_FILE_TYPES.contains(fileType))
        .collect(Collectors.joining(","));
  }
}
