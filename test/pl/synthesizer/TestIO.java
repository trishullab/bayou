import edu.rice.bayou.annotations.Evidence;

public class TestIO {

    @Evidence(apicalls = {"readLine"})
    @Evidence(types = {"FileReader", "BufferedReader"})
    void __bayou_fill(String file) {

    }   

}
