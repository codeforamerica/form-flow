package formflow.library.file;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * This implementation of <code>CloudFileRepository</code> does nothing useful at all. It logs the methods being called.
 */
@Service
@Profile("test")
@Slf4j
public class NoOpCloudFileRepository implements CloudFileRepository {

    /**
     * Default constructor.
     */
    public NoOpCloudFileRepository() {
    }

    @Override
    public void upload(String filepath, MultipartFile file) throws IOException, InterruptedException {
        log.info("Pretending to upload file {} to s3 with filepath {}", file.getOriginalFilename(), filepath);
    }

    @Override
    public CloudFile get(String filepath) {
        log.info("Pretending to get file {}", filepath);
        return null;
    }

    @Override
    public void delete(String filepath) {
        log.info("Pretending to delete file from s3: {}", filepath);
    }
}
