import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.net.wifi.WifiManager;

public class TestWifi {

    /* Start a wi-fi scan using the given manager */
    void scan(WifiManager manager) {
        {
            Evidence.apicalls("startScan");
            Evidence.types("WifiManager");
        }
    }   

}
