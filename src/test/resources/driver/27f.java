// android
import android.bluetooth.BluetoothSocket;

class Test {
    BluetoothSocket socket;
    public Test(BluetoothSocket socket) {
        this.socket = socket;
    }

    public void doTest() {
        try {
            socket.connect();
        } catch (IOException e) {
            close();
        }
    }

    private void close() {
        socket.close();
    }
}
