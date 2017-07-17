import edu.rice.cs.caper.bayou.annotations.Evidence;

public class TestIO {

    /* NOTE: Please use only only inference query at a time,
     * and comment out the rest */

    // Read from a file
    void read(String file) {
        Evidence.apicalls("readLine");
    }   

    /*
    // Read from a file, more specifically using the
    // string argument given
    void read(String file) {
        Evidence.apicalls("readLine");
        Evidence.context("String");
    }   
    */

    /*
    // Read from the file, performing exception handling
    // properly by printing the stack trace
    void readWithErrorHandling() {
        String file;
        {
            Evidence.apicalls("readLine", "printStackTrace", "close");
            Evidence.context("String");
        }
    }   
    */
}
