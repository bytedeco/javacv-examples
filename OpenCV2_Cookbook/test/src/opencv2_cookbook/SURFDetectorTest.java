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
        // JVM crashes in on certain machines executing following code (JVM 1.7.0_17-x64, JavaCV 0.4)
        surf.detect(image, null, keyPoints);
        // Not sure what should be the number of key points since crashes in line above, guessing 11.
        assertEquals(11, keyPoints.capacity());
    }
}