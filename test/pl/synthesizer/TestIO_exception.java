import edu.rice.bayou.annotations.Evidence;

public class TestIO_exception {

    void read() {
        String file;
	{
	    Evidence.apicalls("readLine", "printStackTrace");
	    Evidence.context("String");
	}
    }   

}
