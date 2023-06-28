package formflow.library.upload;


import java.io.File;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CloudFile {

  Long fileSize;
  File file;
}
