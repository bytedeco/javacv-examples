/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._
import com.googlecode.javacv.cpp.opencv_highgui._
import java.io.File

/**
 * Modifies image using histogram equalization.
 * Example for section "Equalizing the image histogram" in Chapter 4.
 */
object Ex5EqualizeHistogram extends App {

    // Load image as a grayscale
    val src = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_GRAYSCALE)
    // Show histogram of the source image
    show(new Histogram1D().getHistogramImage(src), "Input histogram");

    // Apply look-up
    val dest = Histogram1D.equalize(src)

    // Show inverted image
    show(dest, "Equalized Histogram")
    // Show histogram of the modified image
    show(new Histogram1D().getHistogramImage(dest), "Equalized histogram");
}
