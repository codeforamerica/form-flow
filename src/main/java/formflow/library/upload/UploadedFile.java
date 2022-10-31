package formflow.library.upload;

import lombok.Data;

@Data
public class UploadedFile {

  private String filename;
  private String filePath;
  private String thumbnailFilepath;
  private String type;
  private long size; // bytes
  private String sysFileName;

  public UploadedFile(String filename, String filePath, String thumbnailFilepath, String type, long size) {
    this.filename = filename;
    this.filePath = filePath;
    this.thumbnailFilepath = thumbnailFilepath;
    this.type = type;
    this.size = size;
  }
}
