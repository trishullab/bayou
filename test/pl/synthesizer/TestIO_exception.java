import edu.rice.bayou.annotations.Evidence;

public class TestIO_exception {

    @Evidence(apicalls = {"readLine", "printStackTrace"})
    @Evidence(types = {"FileReader", "BufferedReader"})
    void __bayou_fill(String file) {

    }   

}
