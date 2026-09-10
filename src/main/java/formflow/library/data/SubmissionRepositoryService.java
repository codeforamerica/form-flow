package formflow.library.data;

import static formflow.library.config.submission.ShortCodeConfig.Config.ShortCodeType.alpha;
import static formflow.library.config.submission.ShortCodeConfig.Config.ShortCodeType.alphanumeric;
import static formflow.library.config.submission.ShortCodeConfig.Config.ShortCodeType.numeric;

import com.google.common.hash.Hashing;
import formflow.library.config.submission.ShortCodeConfig;
import formflow.library.config.submission.ShortCodeConfig.Config.ShortCodeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service to retrieve and store {@link formflow.library.data.Submission} objects in the database.
 */
@Service
@Transactional
@Slf4j
public class SubmissionRepositoryService {

    /**
     * The database's UNIQUE constraint on short_code is the real arbiter of uniqueness across concurrent app
     * instances - existsByShortCode alone can't prevent two instances from both passing the check for the same
     * code. This bounds how many times generateAndSetUniqueShortCode retries after a genuine collision before
     * giving up, so a misconfigured (too-small) code space can't loop forever.
     */
    private static final int MAX_SHORT_CODE_ATTEMPTS = 50;

    SubmissionRepository repository;

    SubmissionEncryptionService encryptionService;

    ShortCodeConfig shortCodeConfig;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * A single failed save (e.g. a short code collision) marks the enclosing Postgres transaction as aborted -
     * every subsequent statement in it fails until rollback. Each short-code attempt needs its own independent
     * transaction so one collision doesn't poison the retries that follow it.
     */
    private final TransactionTemplate requiresNewTransactionTemplate;

    public SubmissionRepositoryService(SubmissionRepository repository, SubmissionEncryptionService encryptionService,
            ShortCodeConfig shortCodeConfig, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.shortCodeConfig = shortCodeConfig;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static String generateRandomCode(int length, ShortCodeType type) {
        RandomStringGenerator.Builder builder = RandomStringGenerator.builder().withinRange('0', 'z');

        builder = switch (type) {
            case alphanumeric -> builder.filteredBy(Character::isLetterOrDigit);
            case alpha -> builder.filteredBy(Character::isLetter);
            case numeric -> builder.filteredBy(Character::isDigit);
        };

        return builder.get().generate(length);
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
        // Hibernate can defer the UPDATE (and the resulting version bump) until the transaction
        // flushes. Force it now so the version we read below - and hand back to the caller - is
        // never stale relative to what's actually in the database.
        entityManager.flush();
        if (newRecord) {
            log.info("created submission id: " + savedSubmission.getId());
        }
        // straight from the db will be encrypted, so decrypt first.
        return encryptionService.decrypt(savedSubmission);
    }

    /**
     * Runs {@code action} while holding a Postgres transaction-scoped advisory lock keyed on {@code lockKey}. The lock is
     * acquired via {@code pg_advisory_xact_lock}, so it is automatically released when the current transaction commits or
     * rolls back, and it serializes concurrent callers across every JVM/instance sharing the same database, not just threads
     * within this process.
     *
     * @param lockKey a string identifying the resource being protected, e.g. a session id + flow combination
     * @param action  the action to run once the lock is held
     * @param <T>     the type returned by {@code action}
     * @return whatever {@code action} returns
     */
    @Transactional
    public <T> T withSubmissionLock(String lockKey, Supplier<T> action) {
        long lockId = Hashing.sha256().hashString(lockKey, StandardCharsets.UTF_8).asLong();
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?1)")
                .setParameter(1, lockId)
                .getSingleResult();
        return action.get();
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateAndSetUniqueShortCode(Submission submission) {

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
        int attempts = 0;
        do {
            String newCode = switch (config.getCodeType()) {
                case alphanumeric -> generateRandomCode(codeLength, alphanumeric);
                case alpha -> generateRandomCode(codeLength, alpha);
                case numeric -> generateRandomCode(codeLength, numeric);
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

            attempts++;
            boolean exists = repository.existsByShortCode(newCode);
            if (!exists) {
                // If the newly generated code isn't already in the database being used by a prior submission
                // set this submission's shortcode, and persist it. Each attempt runs in its own fresh
                // transaction (PROPAGATION_REQUIRES_NEW): a Postgres constraint violation aborts the whole
                // enclosing transaction, so retrying in the same transaction as a failed attempt would just
                // fail every statement after it with "current transaction is aborted."
                String candidateCode = newCode;
                try {
                    requiresNewTransactionTemplate.executeWithoutResult(status -> {
                        submission.setShortCode(candidateCode);
                        save(submission);
                    });
                    log.info("Created short code {} for submission {}", candidateCode, submission.getId());
                } catch (ConstraintViolationException | DataIntegrityViolationException e) {
                    // A concurrent request/instance won the race and inserted this same code between our
                    // exists-check and our save - existsByShortCode alone can't prevent that across instances.
                    // The unique constraint is the real arbiter; reset and try another candidate. Save's
                    // explicit entityManager.flush() means the violation surfaces as Hibernate's raw
                    // ConstraintViolationException here rather than Spring's translated
                    // DataIntegrityViolationException - caught for both, in case that ever changes.
                    log.warn("Short code {} collided with a concurrently-created submission for {}; retrying.",
                            candidateCode, submission.getId());
                    submission.clearShortCodeAfterFailedSave();
                }
            } else {
                log.warn("Confirmation code {} already exists", newCode);
            }
        } while (submission.getShortCode() == null && attempts < MAX_SHORT_CODE_ATTEMPTS);

        if (submission.getShortCode() == null) {
            log.error("Unable to generate a unique short code for submission {} after {} attempts", submission.getId(),
                    attempts);
        }
    }
}
