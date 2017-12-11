import org.junit.*;

import java.io.*;
import java.util.function.*;

/**
 * Defines JUnit test cases to check that sythesized code provides expected runtime behavior.
 */
public abstract class TestSuite
{

    @Test
    public void Test1() throws IOException
    {
        File f = File.createTempFile("temp-file", ".tmp");
        f.deleteOnExit(); // ensure the file is removed on exit
        makeTestable().accept(f.toString());
        Assert.assertFalse("Expecting file to be deleted, but file still exists.", f.exists());
    }

	protected abstract Consumer<String> makeTestable();
}

