import edu.rice.cs.caper.bayou.annotations.Evidence;

public class TestIO_exception {

    void readWithErrorHandling() {
        String file;
        {
            Evidence.apicalls("readLine", "printStackTrace", "close");
            Evidence.context("String");
        }
    }   

}
