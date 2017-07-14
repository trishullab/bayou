import edu.rice.cs.caper.bayou.annotations.Evidence;

public class TestIO {

    void read(String file) {
        Evidence.apicalls("readLine");
    }   

    /*
    void read(String file) {
        Evidence.apicalls("readLine");
        Evidence.context("String");
    }   
    */

    /*
    void readWithErrorHandling() {
        String file;
        {
            Evidence.apicalls("readLine", "printStackTrace", "close");
            Evidence.context("String");
        }
    }   
    */
}
