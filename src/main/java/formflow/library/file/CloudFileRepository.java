package formflow.library.file;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;


public interface CloudFileRepository {

    void upload(String filePath, MultipartFile file) throws IOException, InterruptedException;

    CloudFile get(String filepath);

    void delete(String filepath);
}