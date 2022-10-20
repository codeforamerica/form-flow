package formflow.library.upload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class TransferManagerConfiguration {

  @Bean
  TransferManager transferManager(@Autowired AmazonS3 s3Client) {
    return TransferManagerBuilder.standard()
        .withS3Client(s3Client)
        .build();
  }
}
