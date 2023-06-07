package formflow.library.data;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.aead.AeadConfig;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static formflow.library.utils.SubmissionUtils.copySubmission;

@Service
public class SubmissionEncryptionService {

  public final String ENCRYPT_SUFFIX = "_encrypted";
  private final Aead encDec;

  @Value("${form-flow.inputs: 'org.formflowstartertemplate.app.inputs'}")
  private String inputConfigPath;

  public SubmissionEncryptionService(@Value("${form-flow.encryption-key:}") String key) {
    try {
      AeadConfig.register();
      encDec = CleartextKeysetHandle.read(JsonKeysetReader.withString(key))
        .getPrimitive(Aead.class);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Go through "@Encrypt" annotations and replace fields with encrypted value
   *
   * @param submission submission to be encrypted
   * @return
   */
  public Submission encrypt(Submission submission) {
    if (submission.getFlow() == null) {
      return submission;
    }
    Submission result = copySubmission(submission);

    try {
      List<Field> fields = getAnnotatedFields(submission);
      return replaceValues(result, fields, true);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to find flow class", e);
    }

  }


  public Submission decrypt(Submission submission) {
    if (submission.getFlow() == null) {
      return submission;
    }
    Submission result = new Submission(submission);

    try {
      List<Field> fields = getAnnotatedFields(submission);
      return replaceValues(result, fields, false);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to find flow class", e);
    }

  }

  @NotNull
  private Submission replaceValues(Submission result, List<Field> fields, boolean doEncryption) {
    Map<String, Object> inputData = result.getInputData();
    for (Map.Entry<String, Object> entry : inputData.entrySet()) {
      for (Field field : fields) {
        if (entry.getKey().equals(field.getName())) {
          replaceValue(field, inputData, doEncryption);
        } else if (entry.getValue() instanceof List) {
          // check for subflows
          for (var element : (List) entry.getValue()) {
            if (element instanceof Map) {
              Map<String, Object> subflow = (Map<String, Object>) element;
              if (subflow.containsKey(field.getName() + ENCRYPT_SUFFIX)) {
                replaceValue(field, subflow, doEncryption);
              }
            } else {

            }
          }
        }
      }
    }

    return result;
  }

  @NotNull
  private List<Field> getAnnotatedFields(Submission submission) throws ClassNotFoundException {
    Class<?> flowClass = Class.forName(inputConfigPath + StringUtils.capitalize(submission.getFlow()));

    List<Field> fields = Arrays.stream(flowClass.getDeclaredFields())
      .filter(field -> Arrays.stream(field.getAnnotations())
        .anyMatch(annotation -> annotation.annotationType().getName().endsWith(".Encrypted")))
      .toList();
    return fields;
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

  private void replaceValueEncryptedValue(Field field, Map<String, Object> inputMap) {
    String value = (String) inputMap.remove(field.getName());
    inputMap.put(field.getName() + ENCRYPT_SUFFIX, encryptString(value));
  }

  private void replaceValueDecryptedValue(Field field, Map<String, Object> inputMap) {
    String value = (String) inputMap.remove(field.getName());
    inputMap.put(field.getName().replace(ENCRYPT_SUFFIX, ""), decryptString(value));
  }

  private void replaceValue(Field field, Map<String, Object> inputMap, boolean encrypt) {
    if (encrypt) {
      replaceValueEncryptedValue(field, inputMap);
    } else {
      replaceValueDecryptedValue(field, inputMap);
    }
  }

}
