/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter06


import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._


/**
 * Computation of Laplacian and zero-crossing.
 * Helper class for section "Computing the Laplacian of an image" in Chapter 6, page 156,
 * used in `Ex4Laplacian`.
 */
class LaplacianZC {

    /**
     * Aperture size of the Laplacian kernel
     */
    var aperture = 5

    private var laplace: CvMat = null

    /**
     * Compute floating point Laplacian.
     */
    def computeLaplacian(src: IplImage): IplImage = {

        val dest = cvCreateImage(cvGetSize(src), IPL_DEPTH_32F, src.nChannels())
        cvLaplace(src, dest, aperture)

        laplace = dest.asCvMat()

        dest
    }

    /**
     * Get binary image of the zero-crossings
     * if the product of the two adjustment pixels is
     * less than threshold then this is a zero crossing
     * will be ignored.
     */
    def getZeroCrossings(threshold: Double = 1): IplImage = {

        val cols = laplace.cols()
        val rows = laplace.rows()

        // Binary image initialize to white (255)
        val dest = cvCreateMat(rows, cols, CV_8U)
        for (i <- 0 until rows * cols) dest.put(i, 255)

        // Negate the input threshold value
        val t = threshold * -1

        // If a product of two adjacent pixel values is negative then there is a zero-crossing.
        // Do vertical and horizontal tests.
        for (c <- 1 until cols; r <- 1 until rows) {
            val v = laplace.get(r, c)
            if ((v * laplace.get(r, c - 1) < t) || (v * laplace.get(r - 1, c) < t)) {
                // There is a zero-crossing and its straight is above the threshold
                // Set it to black (0)
                dest.put(r, c, 0)
            }
        }

        dest.asIplImage()
    }

}
