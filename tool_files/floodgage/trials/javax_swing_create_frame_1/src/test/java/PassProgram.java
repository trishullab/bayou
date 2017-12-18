import java.util.function.*;
import java.util.*;

import javax.swing.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */

public class PassProgram implements Consumer<String> { 
    public void accept(String title)
    {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true); 
    } 
}
