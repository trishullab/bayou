import java.util.Stack;
import java.util.function.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements Consumer<Stack<Integer>>
{
    public void accept(Stack<Integer> stack) {
        while (! stack.isEmpty())
            stack.pop();
    }
}


