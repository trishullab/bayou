import java.io.*;
import java.util.function.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements BiConsumer<File, String>
{
    public void accept(File file, String content) {
        try {
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) { }
    }
}


