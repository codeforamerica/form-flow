package formflow.library.inputs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field on a flow's input class (e.g. an SSN field) as one whose value should be encrypted at rest and
 * decrypted when read back. {@link formflow.library.data.SubmissionEncryptionService} looks for this annotation
 * to decide which fields in a {@link formflow.library.data.Submission}'s input data to encrypt/decrypt.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Encrypted {

    /**
     * Reserved for future use; not currently read by {@link formflow.library.data.SubmissionEncryptionService}.
     *
     * @return the reserved key value, currently unused
     */
    String key() default "";
}