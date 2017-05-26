import edu.rice.bayou.annotations.Evidence;
import android.content.Context;

public class TestDialog {

    @Evidence(apicalls = {"setTitle", "setMessage", "create"})
    @Evidence(types = {"AlertDialog", "Builder"})
    void __bayou_fill(Context c, String str1, String str2) {

    }   

}
