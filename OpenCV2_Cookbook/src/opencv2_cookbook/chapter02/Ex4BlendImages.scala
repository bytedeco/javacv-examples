/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter02

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Blend two images using weighted addition.
 *
 * This example demonstrates image arithmetic, in particular weighted addition that is very useful for image blending.
 */
object Ex4BlendImages extends App {

    // Read input images
    val image1 = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)
    val image2 = loadAndShowOrExit(new File("data/rain.jpg"), CV_LOAD_IMAGE_COLOR)

    // Define output image
    val result = IplImage.create(cvGetSize(image1), image1.depth, 3)

    // Create blended image
    cvAddWeighted(image1, 0.7, image2, 0.9, 0.0, result)

    // Display
    show(result, "Blended")
}