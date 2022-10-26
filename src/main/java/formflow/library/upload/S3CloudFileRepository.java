package formflow.library.upload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class S3CloudFileRepository implements CloudFileRepository {

  private final String bucketName;

  private final TransferManager transferManager;

  public S3CloudFileRepository() {
    AWSCredentials credentials = new BasicAWSCredentials(System.getenv("AWS_ACCESS_KEY"),
        System.getenv("AWS_SECRET_KEY"));
    this.bucketName = System.getenv("S3_BUCKET");
    AmazonS3 s3Client = AmazonS3ClientBuilder
        .standard()
        .withRegion(Regions.US_WEST_1)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
    this.transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();
  }

//  public byte[] get(String filepath) {
//    try {
//      S3Object obj = s3Client.getObject(bucketName, filepath);
//      S3ObjectInputStream stream = obj.getObjectContent();
//      byte[] content = IOUtils.oByteArray(stream);
//      obj.close();
//      return content;
//    } catch (Exception e) {
//      return null;
//    }
//  }

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
      System.err.println(e.getErrorMessage());
      System.exit(1);
    } catch (InterruptedException | IOException e) {
      log.info("Not a AmazonServiceException");
      log.info(e.getMessage());
      throw new RuntimeException(e);
    }
  }

//  public void upload(String filepath, String fileContent) {
//
//  }
//
  public void delete(String filepath) throws SdkClientException {
    log.info("Deleting file at filepath {} from S3", filepath);
//    s3Client.deleteObject(new DeleteObjectRequest(bucketName, filepath));
  }
}
