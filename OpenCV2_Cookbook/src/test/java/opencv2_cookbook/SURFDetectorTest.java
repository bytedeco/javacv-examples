/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook;

import org.bytedeco.javacpp.opencv_core;
import org.junit.Test;

import static org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import static org.bytedeco.javacpp.opencv_highgui.imread;
import static org.bytedeco.javacpp.opencv_nonfree.SURF;
import static org.junit.Assert.assertEquals;

public class SURFDetectorTest {

    @Test
    public void detect() {
        final opencv_core.Mat image = imread("data/church01.jpg");
        final KeyPoint keyPoints = new KeyPoint();
        final SURF surf = new SURF(2500, 4, 2, true, false);
        // JVM crashes when JavaCV native binaries and OpenCV binaries are build with different versions of VisualStudio
        // For instance, JavaCV is build with VC10 and OpenCV with VC11.

        surf.detect(image, keyPoints);
        assertEquals(320, keyPoints.capacity());
    }
}