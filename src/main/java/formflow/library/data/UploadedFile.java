package formflow.library.data;

import static javax.persistence.TemporalType.TIMESTAMP;

import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.stereotype.Component;

/**
 * A class representing what an uploaded file which can be saved in either S3 or Azure
 */
@TypeDef(
    name = "json", typeClass = JsonType.class
)
@Entity
@Table(name = "uploaded_file")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
@Builder
public class UploadedFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CreationTimestamp
  @Temporal(TIMESTAMP)
  @Column(name = "created_at")
  private Date createdAt;

  @UpdateTimestamp
  @Temporal(TIMESTAMP)
  @Column(name = "updated_at")
  private Date updatedAt;

  @Column(name = "original_name")
  private String originalName;

  @Column(name = "repository_path")
  private String repositoryPath;

  @Column(name = "extension")
  private String extension;
}
