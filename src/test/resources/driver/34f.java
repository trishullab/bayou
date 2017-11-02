// recursive methods
import java.util.Stack;
class Test {
    public void test(Stack<String> stack, int n) {
        if (n == 0)
            return;
        stack.push("abcd");
        stack.pop();
        test1(stack, n-1);
    }

    public void test1(Stack<String> stack, int n) {
        if (n == 0)
            return;
        stack.push("efgh");
        stack.pop();
        test(stack, n-1);
    }
}
