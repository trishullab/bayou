import edu.rice.bayou.annotations.Evidence;
import android.bluetooth.BluetoothSocket;

public class TestIO_seqs {

    @Evidence(sequence =
            {
                "java.io.InputStreamReader.InputStreamReader(java.io.InputStream)",
                "java.io.BufferedReader.BufferedReader(java.io.Reader)",
                "java.io.BufferedWriter.BufferedWriter(java.io.Writer)",
                "java.io.BufferedReader.read()",
                "java.io.BufferedWriter.write(int)",
                "java.io.BufferedReader.close()",
                "java.io.BufferedWriter.close()"
            })
    void __bayou_fill(String file, BluetoothSocket socket) {

    }   

}
