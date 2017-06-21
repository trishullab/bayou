// javadoc
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.File;

class Test {
    BufferedReader br;
    public Test(File file) {
        br = new BufferedReader(new FileReader(file));
    }

    /**
     * This is a summary.
     *
     * This however is not a summary. It's the full description
     * and may include linebreaks,
     * <p> html tags
     *
     * @param but not javadoc tags
     */
    public void doTest() {
        String s;
        int i;
        for (i = 0; i < 10; i++)
            System.out.println(i);
        while ((s = br.readLine() != null))
            br.ready();
        br.close();
    }
}
