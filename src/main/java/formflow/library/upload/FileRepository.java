package formflow.library.upload;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import static java.nio.charset.StandardCharsets.UTF_8;



public interface FileRepository {

  byte[] get(String filepath);

  void upload(String filepath, MultipartFile file) throws IOException, InterruptedException;

  void upload(String filepath, String fileContent) throws IOException, InterruptedException;

  void delete(String filepath);

  default String getThumbnail(UploadedFile uploadedFile) {
    try {
      var thumbnailBytes = get(uploadedFile.getThumbnailFilepath());
      return new String(thumbnailBytes, UTF_8);
    } catch (Exception e) {
      return "";
    }
  }
}