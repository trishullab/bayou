import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.content.Context;

public class TestDialog {

    void createDialog(Context c) {
        String str1 = "something here";
        String str2 = "another thing here";
        {
            Evidence.apicalls("setTitle", "setMessage");
            Evidence.types("AlertDialog");
        }
    }   

}
