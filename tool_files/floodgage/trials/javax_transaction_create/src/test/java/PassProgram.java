import java.util.function.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import javax.naming.InitialContext;
import javax.transaction.*;

/**
 * Create here a class eligable to be returned from TestSuite::makeTestable()
 */
public class PassProgram implements Consumer<String>
{
    public void accept(String tran)
    {
        try {
            InitialContext ic = new InitialContext();
            UserTransaction ut = (UserTransaction)ic.lookup(tran);
            ut.begin();

            // do something here
            
            ut.commit();
     
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

