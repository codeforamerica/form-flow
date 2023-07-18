package formflow.library.utils;

import static java.util.Arrays.stream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
public class AcceptedFileTypeUtils {

  static Map<String, String> FILE_EXT_MIME_TYPE_MAP = Map.ofEntries(
          Map.entry(".jpeg", "image/jpeg"),
          Map.entry(".jpg", "image/jpeg"),
          Map.entry(".png", "image/png"),
          Map.entry(".pdf", "application/pdf"),
          Map.entry(".bmp", "image/bmp"),
          Map.entry(".ods", "application/vnd.oasis.opendocument.spreadsheet"),
          Map.entry(".odp", "application/vnd.oasis.opendocument.presentation"),
          Map.entry(".doc", "application/msword"),
          Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
          Map.entry(".gif", "image/gif"),
          Map.entry(".odt", "application/vnd.oasis.opendocument.text")
  );
  static final List<String> ACCEPTED_FILE_TYPES = FILE_EXT_MIME_TYPE_MAP.keySet().stream().toList();

  static final List<String> ACCEPTED_MIME_TYPES = FILE_EXT_MIME_TYPE_MAP.values().stream().distinct().toList();
//  @Value("${form-flow.uploads.accepted-file-types:''}")
//  static String userProvidedFileTypes;

  public static Boolean isAcceptedMimeType(MultipartFile file){
    String contentType = file.getContentType();
    return ACCEPTED_MIME_TYPES.contains(contentType);
  }

  public static String acceptedFileTypes(@Value("${form-flow.uploads.accepted-file-types:''}") String userProvidedFileTypes) {

    log.info(String.format("User provided file types: %s", userProvidedFileTypes));
    if (userProvidedFileTypes == null || userProvidedFileTypes.isEmpty()) {
      return String.join(",", ACCEPTED_FILE_TYPES);
    }

    String diffExtString = stream(userProvidedFileTypes.split(","))
            .distinct()
            .filter(fileType -> !ACCEPTED_FILE_TYPES.contains(fileType))
            .collect(Collectors.joining(","));

    if (!diffExtString.isBlank()) {
      log.warn(String.format("Ignoring invalid file types: %s", diffExtString));
    }

    return stream(userProvidedFileTypes.split(","))
        .distinct()
        .filter(fileType -> ACCEPTED_FILE_TYPES.contains(fileType))
        .collect(Collectors.joining(","));
  }
}
