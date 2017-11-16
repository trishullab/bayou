import java.functions.*;

public class ReadFileLinesDraft implements Consumer<String>
{
    public void accept(String filePath)
    {
        File file = new File(pathname);
        StringBuilder fileContents = new StringBuilder((int)file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");

        List<String> lines = new ArrayList<String>();
        try
        {
            while(scanner.hasNextLine())
            {
                lines.append(scanner.nextLine());
            }
        }
        finally
        {
            scanner.close();
        }
    }
}
