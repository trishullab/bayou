import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;

public class TestSpeech {

    /* Construct a speech regonizer with the provided listener */
    void speechRecognition(Context context, Intent intent, RecognitionListener listener) {
        {
            Evidence.types("SpeechRecognizer");
            Evidence.context("Context");
        }
    }   

}
