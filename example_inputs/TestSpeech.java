import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;

// Bayou supports three types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods
// 3. context - datatypes of variables that the code should use

public class TestSpeech {

    /* Construct a speech regonizer with the provided listener */
    void speechRecognition(Context context, Intent intent, RecognitionListener listener) {
        { // Provide evidence within a separate block
            // Code should make API calls on "SpeechRecognizer"...
            Evidence.types("SpeechRecognizer");
            // ...and use a "Context" as argument
            Evidence.context("Context");
        } // Synthesized code will replace this block
    }   

}
