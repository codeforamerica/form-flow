package formflow.library.file;

import java.net.URL;
import org.springframework.web.multipart.MultipartFile;

public interface FileVirusScanner {
  abstract Boolean doesFileHaveVirus(MultipartFile file);
}
