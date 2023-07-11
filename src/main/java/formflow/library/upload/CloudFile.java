package formflow.library.upload;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CloudFile {

  private Long fileSize;
  private byte[] fileBytes;
}
