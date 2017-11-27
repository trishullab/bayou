import java.util.Iterator;
import java.util.List;
import java.util.function.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements BiConsumer<List<String>, List<String>>
{
    public void accept(List<String> items, List<String> items2) {
        Iterator<String> iterator = items.iterator();
        while (iterator.hasNext()) {
            items2.add(iterator.next());
        }
    }
}


