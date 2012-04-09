/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._

import java.awt.Rectangle
import java.io.File

/**
 * Uses histogram of a region in an grayscale image to create 'template',
 * looks through the whole image to detect pixels that are similar to that template.
 * Example for section "Backprojecting a histogram to detect specific image content" in Chapter 4.
 */
object Ex6ContentDetectionGrayscale extends App {

    // Load image as a grayscale
    val src = loadOrExit(new File("data/waves.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    // Display image with marked ROI
    val rect = new Rectangle(360, 44, 40, 50)
    show(drawOnImage(src, rect), "Input")

    // Define ROI
    src.roi(toIplROI(rect))

    // Compute histogram within the ROI
    val h = new Histogram1D().getHistogram(src)

    // Normalize histogram so the sum of all bins is equal to 1.
    cvNormalizeHist(h, 1)

    // Remove ROI, we will be using full image for the rest
    src.roi(null)

    // Back projection is done using 32 floating point copy of the input image.
    // The output is also 32 bit floating point
    val dest = cvCreateImage(cvGetSize(src), IPL_DEPTH_32F, src.nChannels)
    cvCalcBackProject(Array(toIplImage32F(src)), dest, h)
    cvReleaseHist(h)

    // Show results
    show(scaleTo01(dest), "Backprojection result")
}
