import org.junit.*;

import java.io.*;
import java.util.function.*;

/**
 * Defines JUnit test cases to check that sythesized code provides expected runtime behavior.
 */
public abstract class TestSuite
{

	//
	// Add here the test cases to execrise intances returned by makeTestable().
	//

        @Test
        public void Test1() throws IOException
        {
            File temp = File.createTempFile("temp-file", ".tmp");
            temp.deleteOnExit();
            String content = "file content";

            makeTestable().accept(temp, content);

            // check if file contents are written correctly
            BufferedReader br = new BufferedReader(new FileReader(temp));
            String written = br.readLine();

            Assert.assertTrue(written.trim().equals(content.trim()));
        }

	/**
	 * @return instances of the class to test.
	 */
	protected abstract BiConsumer<File, String> makeTestable();

        // Other common useful signatures for makeTestable that can be swapped out for above:

        // protected abstract Function<Foo,Bar> makeTestable();
        // protected abstract Consumer<Foo> makeTestable();
        // protected abstract Supplier<Foo> makeTestable();
}

