package formflow.library.data;

import static javax.persistence.TemporalType.TIMESTAMP;

import com.vladmihalcea.hibernate.type.json.JsonType;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TypeDef;
import org.springframework.stereotype.Component;

/**
 * A class representing what an uploaded file which can be saved in either S3 or Azure
 */
@TypeDef(
    name = "json", typeClass = JsonType.class
)
@Entity
@Table(name = "user_files")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Component
@Builder
public class UserFile {

  public static final List<String> SUPPORTS_THUMBNAIL = List.of("image/jpeg", "image/jpg", "image/png", "image/bmp", "image/gif");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long file_id;

  @ManyToOne
  @JoinColumn(name = "submission_id")
  private Submission submission_id;

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

  @Column(name = "mime_type")
  private String mimeType;

  @Column
  private Float filesize;

  public static Float calculateFilesizeInMb(Long fileSize) {
    DecimalFormat df = new DecimalFormat("0.00");
    Float unroundedMb = Float.valueOf(String.valueOf(fileSize / (1024.0 * 1024.0)));
    return Float.valueOf(df.format(unroundedMb));
  }

  public static boolean isSupportedImage(String mimeType) {
    return UserFile.SUPPORTS_THUMBNAIL.contains(mimeType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    UserFile userFile = (UserFile) o;
    return file_id != null && Objects.equals(file_id, userFile.file_id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
