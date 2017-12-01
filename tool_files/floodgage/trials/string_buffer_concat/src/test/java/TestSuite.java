import org.junit.*;
import java.util.function.*;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Defines JUnit test cases to check that sythesized code provides expected runtime behavior.
 */
public abstract class TestSuite
{

	//
	// Add here the test cases to execrise intances returned by makeTestable().
	//

        @Test(timeout=10000)
        public void Test1()
        {
                Function<List<String>,String> fun = makeTestable();
                assertEquals("abcd", fun.apply(Arrays.asList("a","b","c","d")));
        }

        protected abstract Function<List<String>,String> makeTestable();
}

