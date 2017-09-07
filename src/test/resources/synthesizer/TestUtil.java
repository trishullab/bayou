import edu.rice.cs.caper.bayou.annotations.Evidence;

// Bayou supports three types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods
// 3. context - datatypes of variables that the code should use

public class TestUtil {

    // NOTE: Bayou only supports one synthesis task in a given
    // program at a time, so please comment out the rest.

    /* Store a key value pair in a map */
    void store(String key, String value) {
        { // Provide evidence within a separate block
            // Code should call "put"
            Evidence.apicalls("put");
        } // Synthesized code will replace this block
    }   
}
