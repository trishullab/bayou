import edu.rice.bayou.annotations.Evidence;
import android.content.Context;

public class TestDialog_seqs {

    @Evidence(keywords = "set the title message and create an Android dialog using a builder")
    @Evidence(sequence =
            {
                "android.app.AlertDialog.Builder.Builder(android.content.Context)",
                "android.app.AlertDialog.Builder.setMessage(java.lang.CharSequence)",
                "android.app.AlertDialog.Builder.setTitle(java.lang.CharSequence)",
                "android.app.AlertDialog.Builder.create()"
            })
    void __bayou_fill(Context c, String str1, String str2) {

    }   

}
