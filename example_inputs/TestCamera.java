import edu.rice.cs.caper.bayou.annotations.Evidence;

public class TestCamera {

    /* Start a preview of the camera, by setting the
     * preview's width and height using the given ints */
    void preview() {
        int width = 640;
        int height = 480;
        {
            Evidence.apicalls("startPreview");
            Evidence.context("int");
        }
    }   

}
