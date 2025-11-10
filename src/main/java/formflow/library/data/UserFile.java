package formflow.library.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.SourceType;
import org.springframework.stereotype.Component;

/**
 * A class representing what an uploaded file which can be saved in either S3 or Azure
 */

@Entity
@Table(name = "user_files")
@DynamicInsert
@Getter
@Setter
@ToString
@AllArgsConstructor
@Component
@Builder
public class UserFile {

    @Id
    private UUID fileId;
    @ManyToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "original_name")
    private String originalName;
    @Column(name = "repository_path")
    private String repositoryPath;
    @Column(name = "mime_type")
    private String mimeType;
    @Column
    private Float filesize;
    @Column(name = "virus_scanned")
    private boolean virusScanned;
    @Column(name = "doc_type_label")
    private String docTypeLabel;
    @Column(name = "conversion_source_file_id")
    private UUID conversionSourceFileId;

    /**
     * Default constructor.
     */
    public UserFile() {
    }

    /**
     * Creates a HashMap representation of an uploaded user file that holds information about the file (original file name, file
     * size, thumbnail and mime type) which we add to the session for persisting user file uploads when a user refreshes the page
     * or navigates away.
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
        fileInfo.put("docTypeLabel", userFile.getDocTypeLabel());
        return fileInfo;
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
        return fileId != null && Objects.equals(fileId, userFile.fileId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
