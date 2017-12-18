import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements Consumer<BufferedReader>
{
    public void accept(BufferedReader br) {
        try {
            while (br.readLine() != null) {
            }
        } catch (IOException e) { }
    }
}


