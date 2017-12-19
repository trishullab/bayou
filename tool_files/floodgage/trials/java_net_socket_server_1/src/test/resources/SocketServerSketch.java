import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;
import java.io.*;

public class SocketServerSketch
{
    public void accept(String content)
    {
        try {
            ServerSocket listener = new ServerSocket(9090);
            try {
                while (true) {
                    Socket socket = listener.accept();
                    try {
                        OutputStream out = socket.getOutputStream();
                        out.write(content.getBytes(Charset.forName("UTF-8")));
                    } finally {
                        socket.close();
                    }
                }
            }
            finally {
                listener.close();
            }
        } catch(IOException ioe) {
        }
    }
}  
