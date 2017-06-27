import edu.rice.bayou.annotations.Evidence;

public class TestIO_exception {

    @Evidence(apicalls = {"readLine", "printStackTrace", "close"})
    @Evidence(context = {"String"})
    void __bayou_fill(String file) {

    }

}
