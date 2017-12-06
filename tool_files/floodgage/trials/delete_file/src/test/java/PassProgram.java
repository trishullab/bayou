import java.io.*;
import java.util.function.*;

public class PassProgram implements Consumer<String> {
    public void accept(String filename) {
        File file = new File(filename);
        file.delete();
    }
}


