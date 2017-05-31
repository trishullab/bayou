import edu.rice.bayou.annotations.Evidence;

public class TestIO_seqs {

    @Evidence(keywords = "read buffered line from the file")
    @Evidence(sequence =
            {
                "java.io.FileReader.FileReader(java.lang.String)",
                "java.io.BufferedReader.BufferedReader(java.io.Reader)",
                "java.io.BufferedReader.readLine()",
                "java.io.BufferedReader.readLine()",
                "java.io.BufferedReader.close()",
                "java.lang.Throwable.printStackTrace()"
            })
    void __bayou_fill(String file) {

    }   

}
