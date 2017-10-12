// Bayou supports two types of evidence:
// 1. apicalls - API methods the code should invoke
// 2. types - datatypes of objects which invoke API methods

public class TestCamera {

    /* Start a preview of the camera, by setting the
     * preview's width and height using the given ints */
    void preview() {
        // Intersperse code with evidence
        int width = 640;
        int height = 480;

        { // Provide evidence within a separate block
            // Code should call "startPreview" and use "Camera"
            /// call:startPreview type:Camera
        } // Synthesized code will replace this block
    }   

}
