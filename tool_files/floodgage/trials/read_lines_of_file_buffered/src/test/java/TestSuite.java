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
            try(  PrintWriter out = new PrintWriter(temp.getAbsolutePath()))
            {
                out.println("file contents");
            }

            BufferedReader br = new BufferedReader(new FileReader(temp));
            makeTestable().accept(br);

            // if file was fully read then there should be nothing else to read in buffer
            Assert.assertFalse(br.ready());
        }

	/**
	 * @return instances of the class to test.
	 */
	protected abstract Consumer<BufferedReader> makeTestable();

        // Other common useful signatures for makeTestable that can be swapped out for above:

        // protected abstract Function<Foo,Bar> makeTestable();
        // protected abstract Consumer<Foo> makeTestable();
        // protected abstract Supplier<Foo> makeTestable();
}

