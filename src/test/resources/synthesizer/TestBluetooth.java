import edu.rice.bayou.annotations.Evidence;
import android.bluetooth.BluetoothAdapter;

public class TestBluetooth {

    void readFromBluetooth(BluetoothAdapter adapter) {
        String address = "00:43:A8:23:10:F0";
        {
            Evidence.apicalls("getInputStream");
            Evidence.types("BluetoothSocket");
        }
    }   

}
