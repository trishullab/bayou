import java.io.*;
import java.util.function.*;

public class PassProgram implements BiConsumer<String,String> {
    public void accept(String filename1, String filename2) {
        try {
            FileInputStream fis = new FileInputStream(filename1);
            FileOutputStream fos = new FileOutputStream(filename2);
            int b;
            while  ((b=fis.read()) != -1)
                fos.write(b);
            fis.close();
            fos.close();
        } catch (Exception e) { throw new RuntimeException(e);}
    }
}


