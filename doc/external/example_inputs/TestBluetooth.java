import android.bluetooth.BluetoothAdapter;

// Bayou supports two types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods

public class TestBluetooth {

    /* Get an input stream that can be used to read from
     * the given blueooth hardware address */
    void readFromBluetooth(BluetoothAdapter adapter) {
        // Intersperse code with evidence
        String address = "00:43:A8:23:10:F0";

        { // Provide evidence within a separate block
            // Code should call "getInputStream"
            // and use the "BluetoothSocket' type
            /// call:getInputStream type:BluetoothSocket
        } // Synthesized code will replace this block
    }   

}
