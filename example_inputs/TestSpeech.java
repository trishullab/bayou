import edu.rice.cs.caper.bayou.annotations.Evidence;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;

public class TestSpeech {

    void speechRecognition(Context context, Intent intent, RecognitionListener listener) {
        {
            Evidence.types("SpeechRecognizer");
            Evidence.context("Context");
        }
    }   

}
