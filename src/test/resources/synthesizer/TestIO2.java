import edu.rice.bayou.annotations.Evidence;
import java.io.InputStreamReader;

public class TestIO2 {

    @Evidence(types = {"BufferedReader"})
    @Evidence(context = {"Reader"})
    void __bayou_fill(InputStreamReader input) {

    }   

}
