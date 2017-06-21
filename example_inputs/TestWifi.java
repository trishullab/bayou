import edu.rice.bayou.annotations.Evidence;
import android.net.wifi.WifiManager;

public class TestWifi {

    @Evidence(apicalls = {"startScan"})
    @Evidence(types = {"WifiManager"})
    void __bayou_fill(WifiManager manager) {

    }

}
