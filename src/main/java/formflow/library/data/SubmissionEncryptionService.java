package formflow.library.data;

import static formflow.library.data.Submission.copySubmission;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.ConfigurationV0;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service providing encryption and decryption of designated fields in a <code>Submission</code> using Google's Tink Cryptographic
 * Library
 */
@Service
public class SubmissionEncryptionService {

    /**
     * Suffix added to the field name for encrypted fields
     */
    public final String ENCRYPT_SUFFIX = "_encrypted";
    private final Aead encDec;
    private final String inputConfigPath;

    /**
     * Constructs an instance of the <code>SubmissionEncryptionService</code>
     *
     * @param key             the key used to encrypt the fields
     * @param inputConfigPath the package path where inputs classes are located
     */
    public SubmissionEncryptionService(
            @Value("${form-flow.encryption-key:#{null}}") String key,
            @Value("${form-flow.inputs: 'formflow.library.inputs.'}") String inputConfigPath) {
        this.inputConfigPath = inputConfigPath;
        if (key == null || key.isEmpty()) {
            encDec = null;
            return;
        }

        try {
            AeadConfig.register();
            KeysetHandle keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withString(key));
            encDec = keysetHandle.getPrimitive(ConfigurationV0.get(), Aead.class);
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Go through "@Encrypt" annotations and replace fields with encrypted value.
     *
     * <p>Ex.
     * <br>Given:
     * <code>{secret: "this is a secret"}</code>
     * <br>Result:
     * <code>{secret_encrypted: "some-encrypted-chars"}</code>
     * </p>
     *
     * @param submission submission to be encrypted
     * @return encrypted copy of the given submission
     */
    public Submission encrypt(Submission submission) {
        if (submission.getFlow() == null || encDec == null) {
            return submission;
        }

        return getNewSubmission(submission, EncryptionDirection.ENCRYPT);
    }

    /**
     * Goes through "@Encrypted" annotations and replaces the encrypted fields with the decrypted value.
     *
     * <p>Ex.
     * <br>Given:
     * <code>{secret_encrypted: "some-encrypted-chars"}</code>
     * <br>Result:
     * <code>{secret: "this is a secret"}</code>
     * </p>
     *
     * @param submission submission to be decrypted
     * @return decrypted copy of the given submission
     */
    public Submission decrypt(Submission submission) {
        if (submission.getFlow() == null || encDec == null) {
            return submission;
        }

        return getNewSubmission(submission, EncryptionDirection.DECRYPT);
    }

    @NotNull
    private Submission getNewSubmission(Submission submission, EncryptionDirection direction) {
        Submission result = copySubmission(submission);
        List<Field> fields = getAnnotatedFields(submission);
        Map<String, Object> inputData = result.getInputData();
        submission.getInputData().entrySet().forEach(entry -> {
            fields.forEach(field -> {
                if (entry.getValue() instanceof List) {
                    List possibleSubflows = (List) inputData.get(entry.getKey());
                    for (var element : possibleSubflows) {
                        // check for subflows
                        if (element instanceof Map) {
                            Map<String, Object> subflow = (Map<String, Object>) element;
                            if (containsEncryptionField(direction, field, subflow)) {
                                replaceValue(field, subflow, direction);
                            }
                        }
                    }
                } else if (containsEncryptionField(direction, field, entry.getKey())) {
                    replaceValue(field, inputData, direction);
                }
            });
        });

        return result;
    }

    private boolean containsEncryptionField(EncryptionDirection direction, Field field, Map<String, Object> subflow) {
        return (direction == EncryptionDirection.ENCRYPT && subflow.containsKey(field.getName())) ||
                (direction == EncryptionDirection.DECRYPT && subflow.containsKey(field.getName() + ENCRYPT_SUFFIX));
    }

    private boolean containsEncryptionField(EncryptionDirection direction, Field field, String entryKey) {
        return (direction == EncryptionDirection.ENCRYPT && entryKey.equals(field.getName())) ||
                (direction == EncryptionDirection.DECRYPT && entryKey.equals(field.getName() + ENCRYPT_SUFFIX));
    }

    @NotNull
    private List<Field> getAnnotatedFields(Submission submission) {
        try {
            Class<?> flowClass = Class.forName(inputConfigPath + StringUtils.capitalize(submission.getFlow()));

            List<Field> fields = Arrays.stream(flowClass.getDeclaredFields())
                    .filter(field -> Arrays.stream(field.getAnnotations())
                            .anyMatch(annotation -> annotation.annotationType().getName().endsWith(".Encrypted")))
                    .toList();
            return fields;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to find flow class", e);
        }
    }

    private String encryptString(String data) {
        try {
            return new String(Hex.encodeHex(encDec.encrypt(data.getBytes(), null)));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private String decryptString(String encryptedData) {
        try {
            return new String(encDec.decrypt(Hex.decodeHex(encryptedData.toCharArray()), null));
        } catch (GeneralSecurityException | DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    private void replaceValue(Field field, Map<String, Object> inputMap, EncryptionDirection direction) {
        String fieldName = getNewFieldName(field, direction);

        if (direction == EncryptionDirection.ENCRYPT) {
            String value = (String) inputMap.remove(field.getName());
            inputMap.put(fieldName, encryptString(value));
        } else {
            String value = (String) inputMap.remove(field.getName() + ENCRYPT_SUFFIX);
            inputMap.put(fieldName, decryptString(value));
        }
    }

    private String getNewFieldName(Field field, EncryptionDirection direction) {
        if (direction == EncryptionDirection.ENCRYPT) {
            return field.getName() + ENCRYPT_SUFFIX;
        } else {
            return field.getName();
        }
    }

    private enum EncryptionDirection {
        ENCRYPT,
        DECRYPT
    }

}
