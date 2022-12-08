package formflow.library.upload;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;


public interface CloudFileRepository {

  void upload(String filePath, MultipartFile file) throws IOException, InterruptedException;

  void delete(String filepath);
}