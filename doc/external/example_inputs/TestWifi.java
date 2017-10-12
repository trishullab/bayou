import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.net.wifi.WifiManager;

// Bayou supports two types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods

public class TestWifi {

    /* Start a wi-fi scan using the given manager */
    void scan(WifiManager manager) {
        { // Provide evidence within a separate block
            // Code should call "startScan" and use "WifiManager"
            /// call:startScan type:WifiManager
        } // Synthesized code will replace this block

    }   

}
