import edu.rice.cs.caper.bayou.annotations.Evidence;

// Bayou supports two types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods

public class TestIO {

    /* Read from a file */
    void read(String file) {
        { // Provide evidence within a separate block
            // Code should call "readLine" and catch exceptions
            // with printStackTrace
            /// calls:readLine, printStackTrace
        } // Synthesized code will replace this block
    }   
}
