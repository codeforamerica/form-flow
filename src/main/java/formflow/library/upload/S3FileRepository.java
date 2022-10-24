package formflow.library.upload;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class S3FileRepository implements FileRepository {
  private final AWSCredentials credentials = new BasicAWSCredentials(π)
  private final String bucketName;
  private final AmazonS3 s3Client;

  public S3DocumentRepository() {
    this.s3Client = AmazonS3ClientBuilder
        .standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(Regions.US_EAST_2)
        .build();
    this.bucketName = System.getenv("S3_BUCKET");
    this.transferManager = transferManager;
  }

  public byte[] get(String filepath) {
    try {
      S3Object obj = s3Client.getObject(bucketName, filepath);
      S3ObjectInputStream stream = obj.getObjectContent();
      byte[] content = IOUtils.toByteArray(stream);
      obj.close();
      return content;
    } catch (Exception e) {
      return null;
    }
  }

  public void upload(String filepath, MultipartFile file) {
    log.info("Uploading file {} to S3 at filepath {}", file.getOriginalFilename(), filepath);
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(file.getSize());
    try {
      transferManager
          .upload(bucketName, filepath, file.getInputStream(), metadata)
          .waitForCompletion();
      log.info("finished uploading");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void upload(String filepath, String fileContent) {

  }

  public void delete(String filepath) throws SdkClientException {
    log.info("Deleting file at filepath {} from S3", filepath);
    s3Client.deleteObject(new DeleteObjectRequest(bucketName, filepath));
  }
}
