/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.junit.Test;

import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.calcHist;
import static org.junit.Assert.assertNotNull;

public class CalcHistTest {

    @Test
    public void calcHist3D() {
        final Mat image = imread("data/waves.jpg");
        assertNotNull(image);

        // Compute histogram
        final int[] channels = new int[]{0, 1, 2};
        final Mat mask = new Mat();
        final Mat hist = new Mat();
        final int[] histSize = new int[]{8, 8, 8};
        final float[] histRange = new float[]{0f, 255f};
        IntPointer intPtrChannels = new IntPointer(channels);
        IntPointer intPtrHistSize = new IntPointer(histSize);
        final PointerPointer<FloatPointer> ptrPtrHistRange = new PointerPointer<>(histRange, histRange, histRange);
        calcHist(image, 1, intPtrChannels, mask, hist, 3, intPtrHistSize, ptrPtrHistRange, true, false);
    }
}