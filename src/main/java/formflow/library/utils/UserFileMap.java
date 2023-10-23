package formflow.library.utils;

import formflow.library.data.UserFile;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A class to contain the file mapping for the client side rendering of DropZone files.
 * <p>
 * Warning: This class will be serialized and sent to the client side. Do not include sensitive information in here that would
 * then be shared with the client. Only include information that can be shared.
 * </p>
 */
@Slf4j
@Getter
public class UserFileMap {

  // flow -> inputName -> fileId -> file info
  private Map<String, Map<String, Map<UUID, Map<String, String>>>> userFileMap;

  public UserFileMap() {
    userFileMap = new HashMap<>();
  }

  public void addUserFileToMap(String flow, String inputName, UserFile userFile, String thumbDataUrl) {
    Map<String, String> fileInfo = UserFile.createFileInfo(userFile, thumbDataUrl);

    if (!userFileMap.containsKey(flow)) {
      userFileMap.put(flow, new HashMap<>());
    }

    if (!userFileMap.get(flow).containsKey(inputName)) {
      userFileMap.get(flow).put(inputName, new HashMap<>());
    }

    userFileMap.get(flow).get(inputName).put(userFile.getFileId(), fileInfo);
  }

  public void removeUserFileFromMap(String flow, UUID fileId) {

    if (userFileMap.get(flow) == null) {
      log.warn("Unable to remove fileId '{}' from flow '{}'. Flow does not exist",
          fileId.toString(), flow);
      throw new IndexOutOfBoundsException(
          String.format("Flow '%s' does not exist", flow)
      );
    }

    log.debug("Removing fileId '{}' from user file list (flow '{}'", fileId, flow);
    userFileMap.get(flow).forEach((inputField, files) -> {
      files.entrySet().removeIf(e -> e.getKey().equals(fileId));
    });

    // clean up a few things, if that was the last file listed under an inputName or flow name
    userFileMap.get(flow).entrySet().removeIf(e -> e.getValue().isEmpty());

    if (userFileMap.get(flow).isEmpty()) {
      userFileMap.remove(flow);
    }
  }

  public Map<UUID, Map<String, String>> getFiles(String flow, String inputName) {
    return userFileMap.get(flow).get(inputName);
  }
}
