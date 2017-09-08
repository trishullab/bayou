import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.content.Context;

// Bayou supports three types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods
// 3. context - datatypes of variables that the code should use

public class TestDialog {

    /* Create an alert dialog with the given strings
     * as content (title and message) in the dialog */
    void createDialog(Context c) {
        // Intersperse code with evidence
        String str1 = "something here";
        String str2 = "another thing here";

        { // Provide evidence within a separate block
            // Code should call "setTitle" and "setMessage"...
            /// calls: setTitle, setMessage
            // ...on an "AlertDialog" type
            /// types: AlertDialog
        } // Synthesized code will replace this block
    }   

}
