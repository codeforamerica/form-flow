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

  /**
   * Adds a file into the UserFileMap.
   *
   * @param flow         the flow the associate the file with
   * @param inputName    the name of the input the file was gathered through
   * @param userFile     the UserFile to add
   * @param thumbDataUrl the base64 encoded thumbnail image of the file
   */
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

  /**
   * Removes a specific file from the UserFileMap.
   *
   * @param flow   flow the file is apart of
   * @param fileId UUID of the file to remove
   */
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

  /**
   * Utility method to extract the file info map from the UserFileMap. The map maps a file's UUID to its file information. The
   * following information is in the map: 'originalFilename', 'filesize', 'thumbnailUrl', and 'type'.
   *
   * @param flow      flow the files are associated with
   * @param inputName name of the input widget that the files were gathered through
   * @return Returns a map of UserFile UUIDs mapped to key/value string pairs of data about the file
   */
  public Map<UUID, Map<String, String>> getFiles(String flow, String inputName) {
    if (userFileMap.get(flow) == null) {
      log.warn("Unable to get files for flow '{}' for input named '{}'. Flow not found in mapping.", flow, inputName);
      throw new IndexOutOfBoundsException(
          String.format("Flow '%s' does not exist", flow)
      );
    }
    return userFileMap.get(flow).get(inputName);
  }
}
