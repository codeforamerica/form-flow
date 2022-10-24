package formflow.library.upload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.IOUtils;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class S3FileRepository implements FileRepository {

  private final String bucketName;
  private final AmazonS3 s3Client;

  public S3FileRepository(String bucketName, AmazonS3 s3Client) {
    AWSCredentials credentials = new BasicAWSCredentials(System.getenv("AWS_ACCESS_KEY"),
        System.getenv("AWS_SECRET_KEY"));
    this.s3Client = AmazonS3ClientBuilder
        .standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(Regions.US_EAST_2)
        .build();
    this.bucketName = System.getenv("S3_BUCKET");
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

  public void upload(String filepath, File file) {
    TransferManager transferManager = TransferManagerBuilder.standard().build();
    try {
      Upload upload = transferManager.upload(bucketName, filepath, file);
      upload.waitForCompletion();
    } catch (AmazonServiceException e) {
      System.err.println(e.getErrorMessage());
      System.exit(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    transferManager.shutdownNow();
  }

  public void upload(String filepath, String fileContent) {

  }

  public void delete(String filepath) throws SdkClientException {
    log.info("Deleting file at filepath {} from S3", filepath);
//    s3Client.deleteObject(new DeleteObjectRequest(bucketName, filepath));
  }
}
