import org.junit.*;

import java.util.ArrayList;
import java.util.List;
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
        public void Test1()
        {
            List<String> items = new ArrayList<>();
            List<String> items2 = new ArrayList<>();
            items.add("element 1");
            items.add("element 2");
            items.add("element 3");

            makeTestable().accept(items, items2);

            // check if lists are equal
            Assert.assertTrue(items.equals(items2));
        }

	/**
	 * @return instances of the class to test.
	 */
	protected abstract BiConsumer<List<String>, List<String>> makeTestable();

        // Other common useful signatures for makeTestable that can be swapped out for above:

        // protected abstract Function<Foo,Bar> makeTestable();
        // protected abstract Consumer<Foo> makeTestable();
        // protected abstract Supplier<Foo> makeTestable();
}

