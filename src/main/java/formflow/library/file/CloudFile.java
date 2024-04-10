package formflow.library.file;


import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CloudFile {
    private Long fileSize;
    private byte[] fileBytes;
    private Map<String, Object> metadata;
}
