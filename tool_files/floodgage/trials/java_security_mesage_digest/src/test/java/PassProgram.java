import java.util.function.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements Consumer<String>
{
    public void accept(String valStr)
    {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(valStr.getBytes("UTF8"));
    }
}

