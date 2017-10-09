import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;

// Bayou supports two types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods

public class TestSpeech {

    /* Construct a speech regonizer with the provided listener */
    void speechRecognition(Context context, Intent intent, RecognitionListener listener) {
        { // Provide evidence within a separate block
            // Code should make use of the type "SpeechRecognizer"
            /// types:SpeechRecognizer
        } // Synthesized code will replace this block
    }   

}
