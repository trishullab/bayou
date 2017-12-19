import java.security.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.*;

public class CryptoCreateCipherSketch {
    public void accept(String key)
    {
        try {
            Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (Exception ex) {
            throw new CryptoException("Error encrypting/decrypting file", ex);
        }
    }
}
