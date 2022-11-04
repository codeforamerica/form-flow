package formflow.library.upload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class S3CloudFileRepository implements CloudFileRepository {

  private final String bucketName;
  private final TransferManager transferManager;

  public S3CloudFileRepository(@Value("${form-flow.aws.access_key}") String accessKey,
      @Value("${form-flow.aws.secret_key}") String secretKey,
      @Value("${form-flow.aws.s3_bucket_name}") String s3BucketName,
      @Value("${form-flow.aws.region}") String region) {
    AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    this.bucketName = s3BucketName;
    AmazonS3 s3Client = AmazonS3ClientBuilder
        .standard()
        .withRegion(region)
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

  /**
   * Takes a filepath and Multipart file to to upload the multipart file to AWS S3 where the filepath acts as the path to the file
   * in S3. File paths that include "/" will create a folder structure where each string prior to the "/" will represent a folder.
   * @param filePath File path representing a folder structure and path to the file in S3.
   * @param file The multipart file to be uploaded to S3.
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
      System.err.println(e.getErrorMessage());
      System.exit(1);
    } catch (InterruptedException | IOException e) {
      log.info("Not a AmazonServiceException");
      log.info(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Takes a filepath and an image as a base64 encoded string representing a thumbnail. The base64 encoded thumbnail will be uploaded
   * alongside full size images to AWS S3. The filepath acts as the path to the file in S3 where file paths that include "/"
   * will create a folder structure where each string prior to the "/" will represent a folder.
   * @param filePath File path representing a folder structure and path to the file in S3.
   * @param dataURL a base64 encoded string containing the data for a thumnbnail image to be uploaded alongside full size image uploads.
   */
  public void upload(String filePath, String dataURL) {
    byte[] fileContentBytes = dataURL.getBytes(StandardCharsets.UTF_8);
    var byteArrayInputStream = new ByteArrayInputStream(fileContentBytes);
    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setContentType("text/plain");
    objectMetadata.setContentLength(dataURL.getBytes(StandardCharsets.UTF_8).length);
    transferManager.upload(bucketName, filePath, byteArrayInputStream, objectMetadata);
  }

  public void delete(String filepath) throws SdkClientException {
    log.info("Deleting file at filepath {} from S3", filepath);
//    s3Client.deleteObject(new DeleteObjectRequest(bucketName, filepath));
  }
}
