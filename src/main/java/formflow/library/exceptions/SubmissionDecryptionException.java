package formflow.library.exceptions;

/**
 * Thrown by {@link formflow.library.data.SubmissionEncryptionService} when a submission field marked
 * {@link formflow.library.inputs.Encrypted} can't be decrypted.
 */
public class SubmissionDecryptionException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message a message describing which field failed to decrypt
     * @param cause   the underlying decryption failure
     */
    public SubmissionDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
