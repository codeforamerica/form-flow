package formflow.library.file;


import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A CloudFile is a file with a size, bytes, and metadata map
 */
@AllArgsConstructor
@Getter
public class CloudFile {

    /**
     * Default constructor.
     */
    public CloudFile() {
    }

    private Long fileSize;
    private byte[] fileBytes;
    private Map<String, Object> metadata;
}
