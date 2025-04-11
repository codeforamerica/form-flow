package formflow.library.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;

/**
 * This is an implementation of the <code>CloudFileRepository</code> that uses Amazon S3
 */
@Service
@Profile("!test")
@Slf4j
public class S3CloudFileRepository implements CloudFileRepository {

    private final String bucketName;
    private final S3Client s3Client;

    public S3CloudFileRepository(@Value("${form-flow.aws.access_key}") String accessKey,
            @Value("${form-flow.aws.secret_key}") String secretKey,
            @Value("${form-flow.aws.s3_bucket_name}") String s3BucketName,
            @Value("${form-flow.aws.region}") String region) {

        bucketName = s3BucketName;

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        s3Client = S3Client.builder().region(Region.of(region)).credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    /**
     * Takes a filepath and Multipart file to upload the multipart file to AWS S3 where the filepath acts as the path to the file
     * in S3. File paths that include "/" will create a folder structure where each string prior to the "/" will represent a
     * folder.
     *
     * @param filePath File path representing a folder structure and path to the file in S3.
     * @param file     The multipart file to be uploaded to S3.
     */
    public void upload(String filePath, MultipartFile file) {
        try {
            log.info("Inside the S3 File Repository Upload Call");

            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(filePath)
                    .contentType(file.getContentType()).contentLength(file.getSize()).build();

            InputStream inputStream = file.getInputStream();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            log.info("Upload complete");
        } catch (Exception e) {
            // make some noise, something's wrong with our connection to S3
            System.err.println(e.getMessage());
            log.error("AWS S3 exception occurred: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Retrieves a file from S3.
     *
     * @param filepath The path of the file
     * @return CloudFile containing file, file size, and metadata about the file.
     */
    public CloudFile get(String filepath) {
        try {
            log.info("Getting file at filepath {} from S3", filepath);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(filepath).build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);

            GetObjectTaggingRequest taggingRequest = GetObjectTaggingRequest.builder().bucket(bucketName).key(filepath).build();

            GetObjectTaggingResponse taggingResponse = s3Client.getObjectTagging(taggingRequest);
            List<Tag> tagSet = taggingResponse.tagSet();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tags", tagSet);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(s3Object, outputStream);
            byte[] fileBytes = outputStream.toByteArray();
            long fileSize = fileBytes.length;

            log.info("File {} successfully downloaded", filepath);
            return new CloudFile(fileSize, fileBytes, metadata);
        } catch (IOException e) {
            log.error("Exception occurred while attempting to get the file with path %s: " + e.getMessage(), filepath);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Deletes a file from S3 storage.
     *
     * @param filepath The path of the file to delete
     */
    public void delete(String filepath) throws SdkClientException {
        log.info("Deleting file at filepath {} from S3", filepath);
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucketName).key(filepath).build();

        s3Client.deleteObject(deleteRequest);
    }
}
