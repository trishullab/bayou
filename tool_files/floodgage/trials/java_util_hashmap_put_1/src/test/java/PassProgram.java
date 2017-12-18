import java.util.function.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements Consumer<String>
{
    public void accept(String filePath)
    {
        HashMap<Integer, String> id2name = new HashMap<Integer, String>();
        id2name.put(100, "Mike");
        id2name.put(101, "Bobby");
        id2name.remove(101);
    }
}

