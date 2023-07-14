package formflow.library.upload;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AcceptedFileTypeService {

  static List<String> ACCEPTED_FILE_TYPES = List.of(
      ".jpeg", ".jpg", ".png", ".pdf", ".bmp",
      ".gif", ".doc", ".docx", ".odt", ".ods", ".odp");
  private static String userProvidedFileTypes = "";

  public AcceptedFileTypeService(@Value("${form-flow.uploads.accepted-file-types:''}") String userProvidedFileTypes) {
    this.userProvidedFileTypes = userProvidedFileTypes;
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
