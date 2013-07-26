package opencv2_cookbook;

import org.junit.Test;

import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_features2d.KeyPoint;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_nonfree.SURF;
import static org.junit.Assert.assertEquals;

public class SURFDetectorTest {

    @Test
    public void detect() {
        final IplImage image = cvLoadImage("data/church01.jpg");
        final KeyPoint keyPoints = new KeyPoint();
        final SURF surf = new SURF(2500, 4, 2, true, false);
        // JVM crashes when JavaCV native binaries and OpenCV binaries are build with different versions of VisualStudio
        // For instance, JavaCV is build with VC10 and OpenCV with VC11.
        surf.detect(image, null, keyPoints);
        assertEquals(320, keyPoints.capacity());
    }
}