package formflow.library.file;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;


/**
 * Stores, retrieves, and deletes the file bytes for uploaded {@link formflow.library.data.UserFile}s. Implementations
 * choose where that storage actually happens - {@link S3CloudFileRepository} uses AWS S3 in non-test profiles, and a
 * no-op implementation is used for tests.
 */
public interface CloudFileRepository {

    /**
     * Uploads a file's bytes to storage at the given path.
     *
     * @param filePath the path/key to store the file at
     * @param file     the file to upload
     * @throws IOException          if the file's bytes can't be read or the upload fails
     * @throws InterruptedException if the upload is interrupted
     */
    void upload(String filePath, MultipartFile file) throws IOException, InterruptedException;

    /**
     * Retrieves a previously uploaded file's bytes and metadata.
     *
     * @param filepath the path/key the file was stored at
     * @return the file's bytes, size, and metadata
     */
    CloudFile get(String filepath);

    /**
     * Deletes a previously uploaded file from storage.
     *
     * @param filepath the path/key the file was stored at
     */
    void delete(String filepath);
}