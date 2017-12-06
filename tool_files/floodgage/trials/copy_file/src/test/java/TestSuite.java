import org.junit.*;

import java.nio.charset.*;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
import java.util.function.*;

/**
 * Defines JUnit test cases to check that sythesized code provides expected runtime behavior.
 */
public abstract class TestSuite
{
    private static void write(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        bw.close();
    }

    private static String read(File path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path.toString()));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    @Test
    public void Test1() throws Exception
    {
        File f1 = File.createTempFile("temp-file", ".tmp");
        File f2 = File.createTempFile("temp-file", ".tmp");
        f1.deleteOnExit();
        f2.deleteOnExit();
        write(f1, "foo\nbar\n");
        makeTestable().accept(f1.toString(), f2.toString());
        Assert.assertEquals("foo\nbar\n", read(f2));
    }

	protected abstract BiConsumer<String, String> makeTestable();
}

