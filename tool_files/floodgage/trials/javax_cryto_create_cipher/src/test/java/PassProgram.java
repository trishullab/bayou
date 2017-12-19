import java.util.function.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.security.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements Consumer<String>
{
    public void accept(String key)
    {
        try {
            Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (Exception ex) {
           System.out.println("Error encrypting/decrypting file " + ex);
        } 
    }
}

