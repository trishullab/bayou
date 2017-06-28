import edu.rice.bayou.annotations.Evidence;

public class TestIO1 {

    void read(String file) {
        Evidence.apicalls("readLine");
        Evidence.context("String");
    }   

}
