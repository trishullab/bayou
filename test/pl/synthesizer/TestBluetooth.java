import edu.rice.bayou.annotations.Evidence;
import android.bluetooth.BluetoothAdapter;

public class TestBluetooth {

    @Evidence(apicalls = {"connect", "getInputStream"})
    @Evidence(types = {"BluetoothSocket"})
    void __bayou_fill(BluetoothAdapter adapter) {

    }   

}
