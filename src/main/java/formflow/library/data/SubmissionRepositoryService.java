package formflow.library.data;

import formflow.library.config.submission.ShortCodeConfig;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to retrieve and store {@link formflow.library.data.Submission} objects in the database.
 */
@Service
@Transactional
@Slf4j
public class SubmissionRepositoryService {

    SubmissionRepository repository;

    SubmissionEncryptionService encryptionService;

    ShortCodeConfig shortCodeConfig;

    public SubmissionRepositoryService(SubmissionRepository repository, SubmissionEncryptionService encryptionService,
            ShortCodeConfig shortCodeConfig) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.shortCodeConfig = shortCodeConfig;
    }

    /**
     * Saves the Submission in the database.
     *
     * @param submission the {@link formflow.library.data.Submission} to save, not null
     * @return the saved {@link formflow.library.data.Submission}
     */
    public Submission save(Submission submission) {
        var newRecord = submission.getId() == null;
        Submission savedSubmission = repository.save(encryptionService.encrypt(submission));
        if (newRecord) {
            log.info("created submission id: " + savedSubmission.getId());
        }
        // straight from the db will be encrypted, so decrypt first.
        return encryptionService.decrypt(savedSubmission);
    }

    /**
     * Searches for a particular Submission by its {@code id}
     *
     * @param id id of submission to look for, not null
     * @return Optional containing Submission if found, else empty
     */
    public Optional<Submission> findById(UUID id) {
        Optional<Submission> submission = repository.findById(id);
        return submission.map(value -> encryptionService.decrypt(value));
    }

    public Optional<Submission> findByShortCode(String shortCode) {
        Optional<Submission> submission = repository.findSubmissionByShortCode(shortCode);
        return submission.map(value -> encryptionService.decrypt(value));
    }

    /**
     * Removes the CSRF from the Submission's input data, if found.
     *
     * @param submission submission to remove the CSRF from, not null
     */
    public void removeFlowCSRF(Submission submission) {
        submission.getInputData().remove("_csrf");
    }

    /**
     * Removes the CSRF from a particular Submission's Subflow's iteration data, if found.
     * <p>
     * This will remove the CSRF from all the iterations in the subflow.
     * </p>
     *
     * @param submission  submission to look for subflows in, not null
     * @param subflowName the subflow to remove the CSRF from, not null
     */
    public void removeSubflowCSRF(Submission submission, String subflowName) {
        var subflowArr = (ArrayList<Map<String, Object>>) submission.getInputData().get(subflowName);

        if (subflowArr != null) {
            for (var entry : subflowArr) {
                entry.remove("_csrf");
            }
        }
    }

    /**
     * generateAndSetUniqueShortCode generates a read-only unique code for the submission. The short code generation is
     * configurable via {@link formflow.library.config.submission.ShortCodeConfig}:
     * <p>
     * length (default = 6)
     * <p>
     * characterset (alphanumeric, numeric, alpha | default = alphanumeric)
     * <p>
     * forced uppercasing (true, false | default = true)
     * <p>
     * creation point in {@link formflow.library.config.submission.ShortCodeConfig} (creation, submission | default = submission)
     * <p>
     * prefix (default = null)
     * <p>
     * suffix (default = null)
     * <p>
     * This method will check if the generated code exists in the database, and keep trying to create a unique code, before
     * persisting and returning the newly generated code-- therefore it is very important to ensure the configuration allows for a
     * suitably large set of possible codes for the application.
     *
     * @param submission the {@link formflow.library.ScreenController} for which the short code will be generated and saved
     */
    public synchronized void generateAndSetUniqueShortCode(Submission submission) {

        if (submission.getShortCode() != null) {
            log.debug("Unable to create short code for submission {} because one already exists.", submission.getId());
            return;
        }

        log.info("Attempting to create short code for submission {}", submission.getId());

        ShortCodeConfig.Config config = shortCodeConfig.getConfig(submission.getFlow());
        if (config == null) {
            log.error("Unable to find shortcode configuration for flow {}", submission.getFlow());
            return;
        }

        // If there is no short code for this submission in the database, generate one
        int codeLength = config.getCodeLength();
        do {
            String newCode = switch (config.getCodeType()) {
                case alphanumeric -> RandomStringUtils.randomAlphanumeric(codeLength);
                case alpha -> RandomStringUtils.randomAlphabetic(codeLength);
                case numeric -> RandomStringUtils.randomNumeric(codeLength);
            };

            if (config.isUppercase()) {
                newCode = newCode.toUpperCase();
            }

            if (config.getPrefix() != null) {
                newCode = config.getPrefix() + newCode;
            }

            if (config.getSuffix() != null) {
                newCode = newCode + config.getSuffix();
            }

            boolean exists = repository.existsByShortCode(newCode);
            if (!exists) {
                // If the newly generated code isn't already in the database being used by a prior submission
                // set this submission's shortcode, and persist it
                submission.setShortCode(newCode);
                save(submission);
            } else {
                log.warn("Confirmation code {} already exists", newCode);
            }
        } while (submission.getShortCode() == null);
    }
}
