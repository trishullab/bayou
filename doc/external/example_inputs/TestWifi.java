import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.net.wifi.WifiManager;

// Bayou supports three types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods
// 3. context - datatypes of variables that the code should use

public class TestWifi {

    /* Start a wi-fi scan using the given manager */
    void scan(WifiManager manager) {
        { // Provide evidence within a separate block
            // Code should call "startScan"...
            /// call:startScan
            // ...on a "WifiManager" type
            /// type:WifiManager
        } // Synthesized code will replace this block

    }   

}
