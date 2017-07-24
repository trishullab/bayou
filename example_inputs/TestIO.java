import edu.rice.cs.caper.bayou.annotations.Evidence;

// Bayou supports three types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods
// 3. context - datatypes of variables that the code should use

public class TestIO {

    // NOTE: Bayou only supports one synthesis task in a given
    // program at a time, so please comment out the rest.

    /* Read from a file */
    void read(String file) {
        { // Provide evidence within a separate block
            // Code should call "readLine"
            Evidence.apicalls("readLine");
        } // Synthesized code will replace this block
    }   

    /*
    // Read from a file, more specifically using the
    // string argument given
    void read(String file) {
        {
            Evidence.apicalls("readLine");
            Evidence.context("String");
        }
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
