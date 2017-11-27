import org.junit.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<note>\n" +
                    "  <to>Tove</to>\n" +
                    "  <from>Jani</from>\n" +
                    "  <heading>Reminder</heading>\n" +
                    "  <body>Don't forget me this weekend!</body>\n" +
                    "</note>";

            InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
            Assert.assertFalse(inputStream.available() == 0);

            makeTestable().accept(inputStream);

            // check if input stream has been read
            Assert.assertTrue(inputStream.available() == 0);
        }

	/**
	 * @return instances of the class to test.
	 */
	protected abstract Consumer<InputStream> makeTestable();

        // Other common useful signatures for makeTestable that can be swapped out for above:

        // protected abstract Function<Foo,Bar> makeTestable();
        // protected abstract Consumer<Foo> makeTestable();
        // protected abstract Supplier<Foo> makeTestable();
}

