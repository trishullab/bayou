import java.util.*;
import javax.naming.InitialContext;
import javax.transaction.*;   

public class CreateTransactionSketch {
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
