import java.io.*;
import java.util.function.*;

public class PassProgram implements Consumer<String> {
    public void accept(String filename) {
        try {
            File file = new File(filename);
   	        file.createNewFile();
        } catch (IOException e) { }
    }
}


