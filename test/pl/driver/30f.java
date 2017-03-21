// multiple statement switch
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;

class Test {
    BufferedReader br;
    public Test(File file) {
        br = new BufferedReader(new FileReader(file));
    }

    public void doTest() {
        String s;
        switch (br.readLine()){
		case 0:
			br.read();
			br.close();
			break;
		case 1:
			br.reset();
			break;
		default:
			br.close();
	}
    }
}
