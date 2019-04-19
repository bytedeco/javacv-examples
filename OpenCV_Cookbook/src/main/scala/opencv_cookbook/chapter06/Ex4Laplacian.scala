/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter06


import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._


/**
 * The example for section "Computing the Laplacian of an image" in Chapter 6, page 156.
 */
object Ex4Laplacian extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_GRAYSCALE)

    // Compute floating point Laplacian edge strength
    val laplacian = new LaplacianZC()
    laplacian.aperture = 7
    val flap = laplacian.computeLaplacian(src)
    show(toMat8U(flap), "Laplacian")

    // Locate edges using zero-crossing
    val edges = laplacian.getZeroCrossings(flap)
    show(edges, "Laplacian Edges")
}
