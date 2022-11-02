package formflow.library.data;

import static javax.persistence.TemporalType.TIMESTAMP;

import com.vladmihalcea.hibernate.type.json.JsonType;
import java.text.DecimalFormat;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TypeDef;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * A class representing what an uploaded file which can be saved in either S3 or Azure
 */
@TypeDef(
    name = "json", typeClass = JsonType.class
)
@Entity
@Table(name = "user_files")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@Builder
public class UserFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long file_id;

  @ManyToOne(targetEntity = Submission.class)
  @JoinColumn(name = "submission_id", referencedColumnName = "id")
  private Long submission_id;

  @CreationTimestamp
  @Temporal(TIMESTAMP)
  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "original_name")
  private String originalName;

  @Column(name = "repository_path")
  private String repositoryPath;

  @Column(name = "extension")
  private String extension;

  @Column
  private Float filesize;

  public static Float calculateFilesizeInMb(MultipartFile file) {
    DecimalFormat df = new DecimalFormat("0.00");
    Float unroundedMb = Float.valueOf(String.valueOf(file.getSize() / (1024.0 * 1024.0)));
    return Float.valueOf(df.format(unroundedMb));
  }
}
