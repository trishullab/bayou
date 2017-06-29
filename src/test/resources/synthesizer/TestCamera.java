import edu.rice.bayou.annotations.Evidence;

public class TestCamera {

    void preview() {
        int width = 640;
        int height = 480;
        {
            Evidence.apicalls("startPreview");
            Evidence.types("Camera");
        }
    }   

}
