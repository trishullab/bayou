import org.junit.*;

import java.util.Stack;
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
            Stack<Integer> stack = new Stack<>();
            stack.push(1);
            stack.push(2);
            stack.push(3);

            makeTestable().accept(stack);

            // check if stack is empty
            Assert.assertTrue(stack.size() == 0);
        }

	/**
	 * @return instances of the class to test.
	 */
	protected abstract Consumer<Stack<Integer>> makeTestable();

        // Other common useful signatures for makeTestable that can be swapped out for above:

        // protected abstract Function<Foo,Bar> makeTestable();
        // protected abstract Consumer<Foo> makeTestable();
        // protected abstract Supplier<Foo> makeTestable();
}

