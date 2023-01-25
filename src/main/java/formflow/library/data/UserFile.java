package formflow.library.data;

import static javax.persistence.TemporalType.TIMESTAMP;

import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
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
import org.hibernate.annotations.Type;
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

  @Id
  @GeneratedValue
  //@Type(type = "org.hibernate.type.UUIDCharType")
  //@Type(type = "pg-uuid")
  private UUID file_id;

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

  @Column(name = "mime_type")
  private String mimeType;

  @Column
  private Float filesize;

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

  /**
   * Creates a HashMap representation of an uploaded user file that holds information about the file (original file name, file
   * size, thumbnail and mime type) which we add to the session for persisting user file uploads when a user refreshes the page or
   * navigates away.
   *
   * @param userFile          class representing the file the that was uploaded by the user
   * @param thumbBase64String base64 encoded thumbnail of the file the user uploaded
   * @return Hashmap representation of a user file that includes original file name, file size, thumbnail as base64 encoded
   * string, and mime type.
   */
  public static HashMap<String, String> createFileInfo(UserFile userFile, String thumbBase64String) {
    HashMap<String, String> fileInfo = new HashMap<>();
    fileInfo.put("originalFilename", userFile.getOriginalName());
    fileInfo.put("filesize", userFile.getFilesize().toString());
    fileInfo.put("thumbnailUrl", thumbBase64String);
    fileInfo.put("type", userFile.getMimeType());
    return fileInfo;
  }
}
