import edu.rice.bayou.annotations.Evidence;
import android.content.Context;

public class TestDialog {

    void createDialog(Context c) {
        String str1, str2;
	{
	    Evidence.apicalls("setTitle", "setMessage");
	    Evidence.types("AlertDialog");
	}
    }   

}
