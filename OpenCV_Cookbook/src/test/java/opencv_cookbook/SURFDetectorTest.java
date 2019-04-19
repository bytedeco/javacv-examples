/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook;

import org.bytedeco.opencv.opencv_core.KeyPointVector;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_xfeatures2d.SURF;
import org.junit.Test;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.junit.Assert.assertEquals;

public class SURFDetectorTest {

    @Test
    public void detect() {
        final Mat image = imread("data/church01.jpg");
        final KeyPointVector keyPoints = new KeyPointVector();
        final SURF surf = SURF.create(2500, 4, 2, true, false);
        // JVM crashes when JavaCV native binaries and OpenCV binaries are build with different versions of VisualStudio
        // For instance, JavaCV is build with VC10 and OpenCV with VC11.

        surf.detect(image, keyPoints);
        assertEquals(320, keyPoints.size());
    }
}