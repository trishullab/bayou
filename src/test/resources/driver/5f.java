// no constructor, only public method
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;

class Test {
    BufferedReader br;

    public void doTest() {
        br = new BufferedReader(new FileReader(file));
    }
}
