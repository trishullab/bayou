import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.net.wifi.WifiManager;

public class TestWifi {

    void scan(WifiManager manager) {
        {
            Evidence.apicalls("startScan");
            Evidence.types("WifiManager");
        }
    }   

}
