/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter06

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File
import opencv2_cookbook.OpenCVUtils._

/**
 * The example for section "Filtering images using low-pass filters" in Chapter 6, page 142.
 * Basic use of a Gaussian filter.
 */
object Ex1LowPassFilter extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    // Blur with a Gaussian filter
    val dest = cvCreateImage(cvGetSize(src), src.depth, 1)
    val kernelSize = new CvSize(5, 5)
    val sigma = 1.5
    val borderType = BORDER_DEFAULT
    GaussianBlur(src, dest, kernelSize, sigma, sigma, borderType)
    show(dest, "Blured")
}
