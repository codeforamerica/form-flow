package formflow.library.data;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.aead.AeadConfig;

@Service
public class SubmissionEncryptionService {
  private final Aead encDec;

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
   * Go through "Encrypt" annotations and replace fields with encrypted value
   * @param submission submission to be encrypted
   * @return
   */
  public Submission encrypt(Submission submission) {
    return submission;
  }

  public Submission decrypt(Submission submission) {
    return submission;
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
}
