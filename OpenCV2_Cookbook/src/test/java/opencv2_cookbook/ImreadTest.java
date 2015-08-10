/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook;

import org.junit.Test;

import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.junit.Assert.assertNotNull;

public class ImreadTest {

    @Test
    public void readMat() {
        final Mat image = imread("data/church01.jpg");
        assertNotNull(image);
    }
}