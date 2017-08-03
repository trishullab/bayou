// generics with multiple types
import java.util.Map;
import java.util.HashMap;

class Test {
    Map<Integer,String> map;

    public void test() {
        map = new HashMap<Integer,String>();
        map.put(1, "a");
        map.put(2, "b");
        String s = map.get(1);
        map.clear();
    }
}
