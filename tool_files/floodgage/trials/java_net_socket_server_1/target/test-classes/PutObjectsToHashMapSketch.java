import java.util.*;

public class PutObjectsToHashMapSketch {
    public void accept(int id, String name)
    {
        Map id2name = new HashMap<int, String>();
        id2name.put(id, name);
    }
}
