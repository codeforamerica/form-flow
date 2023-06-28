package formflow.library.upload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("!test")
@Slf4j
public class S3CloudFileRepository implements CloudFileRepository {

  private final String bucketName;
  private final AmazonS3 s3Client;
  private final TransferManager transferManager;

  public S3CloudFileRepository(@Value("${form-flow.aws.access_key}") String accessKey,
      @Value("${form-flow.aws.secret_key}") String secretKey,
      @Value("${form-flow.aws.s3_bucket_name}") String s3BucketName,
      @Value("${form-flow.aws.region}") String region) {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    bucketName = s3BucketName;
    s3Client = AmazonS3ClientBuilder
        .standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
    transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
  }

  /**
   * Takes a filepath and Multipart file to upload the multipart file to AWS S3 where the filepath acts as the path to the file in
   * S3. File paths that include "/" will create a folder structure where each string prior to the "/" will represent a folder.
   *
   * @param filePath File path representing a folder structure and path to the file in S3.
   * @param file     The multipart file to be uploaded to S3.
   */
  public void upload(String filePath, MultipartFile file) {
    try {
      log.info("Inside the S3 File Repository Upload Call");
      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setContentType(file.getContentType());
      objectMetadata.setContentLength(file.getSize());
      log.info("Upload Metadata Set");
      Upload upload = transferManager.upload(bucketName, filePath, file.getInputStream(), objectMetadata);
      log.info("Upload Called");
      upload.waitForCompletion();
      log.info("Upload complete");
    } catch (AmazonServiceException e) {
      // make some noise, something's wrong with our connection to S3
      System.err.println(e.getErrorMessage());
      log.error("AWS S3 exception occurred: " + e.getErrorMessage());
      throw new RuntimeException(e.getErrorMessage());
    } catch (InterruptedException | IOException e) {
      log.error("Exception occurred in S3 code: " + e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

  public CloudFile get(String filepath) {
    File file = new File(filepath);
    try {
      log.info("Getting file at filepath {} from S3", filepath);
      Download download = transferManager.download(bucketName, filepath, file);
      ObjectMetadata metadata = download.getObjectMetadata();
      download.waitForCompletion();
      log.info("File {} successfully downloaded", filepath);
      return new CloudFile(metadata.getContentLength(), file);
    } catch (InterruptedException e) {
      log.error("Exception occurred while attempting to get file in S3 code: " + e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

  public void delete(String filepath) throws SdkClientException {
    log.info("Deleting file at filepath {} from S3", filepath);
    s3Client.deleteObject(new DeleteObjectRequest(bucketName, filepath));
  }
}
