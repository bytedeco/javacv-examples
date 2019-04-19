/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.Test;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.junit.Assert.assertNotNull;

public class ImreadTest {

    @Test
    public void readMat() {
        final Mat image = imread("data/church01.jpg");
        assertNotNull(image);
    }
}