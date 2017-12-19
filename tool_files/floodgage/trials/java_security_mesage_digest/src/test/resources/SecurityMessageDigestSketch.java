import java.util.*;

public class SecurityMessageDigestSketch {
    public void accept(String valStr)
    {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(valStr.getBytes("UTF8"));  
    }
}
