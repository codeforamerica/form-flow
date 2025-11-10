package formflow.library.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for UserFile objects
 */
@Repository
public interface UserFileRepository extends JpaRepository<UserFile, UUID> {

    /**
     * Finds all the {@link UserFile}s associated with a {@link Submission}}
     *
     * @param submission the {@link Submission} for which the associated {@link UserFile}s are sought
     * @return {@link List} of associated {@link UserFile} objects
     */
    List<UserFile> findAllBySubmission(Submission submission);

    /**
     * Gets a count of all the {@link UserFile}s associated with a {@link Submission}} where the file has never been converted
     *
     * @param submission the {@link Submission} for which the count of associated {@link UserFile}s are sought
     * @return count of {@link UserFile}s
     */
    long countBySubmissionAndConversionSourceFileIdIsNull(Submission submission);

    /**
     * Finds all the {@link UserFile}s associated with a {@link Submission}} where the conversionSourceFileId matches
     *
     * @param submission             the {@link Submission} for which the associated {@link UserFile}s are sought
     * @param conversionSourceFileId
     * @return
     */
    List<UserFile> findAllBySubmissionAndConversionSourceFileId(Submission submission, UUID conversionSourceFileId);

    /**
     * Finds all the {@link UserFile}s associated with a {@link Submission}} ordered by the OriginalName field
     *
     * @param submission the {@link Submission} for which the associated {@link UserFile}s are sought
     * @return
     */
    List<UserFile> findAllBySubmissionOrderByOriginalName(Submission submission);

    /**
     * Finds all the {@link UserFile}s associated with a {@link Submission}} where the mimeType matches and ordered by the
     * OriginalName field
     *
     * @param submission the {@link Submission} for which the associated {@link UserFile}s are sought
     * @param mimeType
     * @return
     */
    List<UserFile> findAllBySubmissionAndMimeTypeOrderByOriginalName(Submission submission, String mimeType);

    /**
     * Finds all the {@link UserFile}s associated with a {@link Submission}} where the mimeType matches and the
     * conversionSourceFileId is not NULL and ordered by the OriginalName field
     *
     * @param submission the {@link Submission} for which the associated {@link UserFile}s are sought
     */
    List<UserFile> findAllBySubmissionAndMimeTypeAndConversionSourceFileIdIsNotNullOrderByOriginalName(Submission submission,
            String mimeType);
}
