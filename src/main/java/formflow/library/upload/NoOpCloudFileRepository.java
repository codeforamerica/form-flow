package formflow.library.upload;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("test")
@Slf4j
public class NoOpCloudFileRepository implements CloudFileRepository {

  @Override
  public void upload(String filepath, MultipartFile file) throws IOException, InterruptedException {
    log.info("Pretending to upload file {} to s3 with filepath {}", file.getOriginalFilename(), filepath);
  }

  @Override
  public void delete(String filepath) {
    log.info("Pretending to delete file from s3: {}", filepath);
  }
}
