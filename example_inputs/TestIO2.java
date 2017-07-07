import edu.rice.cs.caper.bayou.annotations.Evidence;

public class TestIO2 {

    void read(String file) {
        Evidence.apicalls("readLine");
        Evidence.context("String");
    }   

}
