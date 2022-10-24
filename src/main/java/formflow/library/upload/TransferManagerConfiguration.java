package formflow.library.upload;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class TransferManagerConfiguration {

  @Bean
    //May be a lie?
  TransferManager transferManager(@Autowired AmazonS3 s3Client) {
    return TransferManagerBuilder.standard()
        .withS3Client(s3Client)
        .build();
  }
}
