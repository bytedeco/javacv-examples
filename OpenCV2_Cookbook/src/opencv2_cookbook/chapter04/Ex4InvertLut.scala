/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import java.io.File

/**
 * Creates inverted image by inverting its look-up table.
 * Example for section "Applying look-up table to modify image appearance" in Chapter 4.
 */
object Ex4InvertLut extends App {

    // Load image as a grayscale
    val src = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    // Create inverted lookup table
    val lut = CvMat.create(1, 256, CV_8U)
    for (i <- 0 to 255) {
        lut.put(i, 255 - i)
    }

    // Apply look-up
    val dest = Histogram1D.applyLookUp(src, lut)

    // Show inverted image
    show(dest, "Inverted LUT")
}
