package formflow.library.data;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to retrieve and store UploadedFile objects in the database.
 */
@Service
@Transactional
@Slf4j
public class UserFileRepositoryService {

    /**
     * The {@link UserFileRepository} used by this service
     */
    UserFileRepository repository;

    /**
     * Constructor
     *
     * @param repository the {@link UserFileRepository} used by this service
     */
    public UserFileRepositoryService(UserFileRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves the UploadedFile in the database.
     *
     * @param userFile the uploadedFile to save, not null
     * @return UUID of the file
     */
    public UserFile save(UserFile userFile) {
        return repository.save(userFile);
    }

    /**
     * Searches for a particular UserFile by its {@code id}
     *
     * @param id UUID of the UserFile to look for, not null
     * @return Optional containing UserFile if found, else empty
     */
    public Optional<UserFile> findById(UUID id) {
        return repository.findById(id);
    }

    /**
     * Removes a particular UserFile based on passed in {@code id}
     *
     * @param id UUID of UserFile to remove, not null
     */
    public void deleteById(UUID id) {
        log.info(String.format("Deleting file with id: '%s'", id));
        repository.deleteById(id);
    }

    /**
     * Finds all the {@link UserFile}s associated with a {@link Submission}}
     *
     * @param submission the {@link Submission} for which the associated {@link UserFile}s are sought
     * @return {@link List} of associated {@link UserFile} objects
     */
    public List<UserFile> findAllBySubmission(Submission submission) {
        return repository.findAllBySubmission(submission);
    }

    /**
     * Gets a count of all the uploaded -- not converted --  {@link UserFile}s associated with a {@link Submission}}
     *
     * @param submission the {@link Submission} for which the count of associated {@link UserFile}s are sought
     * @return count of {@link UserFile}s
     */
    public long countOfUploadedFilesBySubmission(Submission submission) {
        return repository.countBySubmissionAndConversionSourceFileIdIsNull(submission);
    }

    public UserFile findBySubmissionAndConversionSourceFileId(Submission submission, UUID ConversionSourceFileId) {
        return repository.findBySubmissionAndConversionSourceFileId(submission, ConversionSourceFileId);
    }
}
