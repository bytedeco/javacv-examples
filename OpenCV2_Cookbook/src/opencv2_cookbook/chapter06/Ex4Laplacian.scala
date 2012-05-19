/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter06


import opencv2_cookbook.OpenCVUtils._

import com.googlecode.javacv.cpp.opencv_highgui._

import java.io.File


/**
 * The example for section "Computing the Laplacian of an image" in Chapter 6, page 156.
 */
object Ex4Laplacian extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    // Compute floating point Laplacian edge strength
    val laplacian = new LaplacianZC()
    laplacian.aperture = 7
    val flap = laplacian.computeLaplacian(src)
    show(toIplImage8U(scaleTo01(flap)), "Laplacian")

    // Locate edges using zero-crossing
    val edges = laplacian.getZeroCrossings(50000f)
    show(edges, "Laplacian Edges")
}
