/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._

import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._

import java.awt.Rectangle
import java.io.File

/**
 * Uses histogram of region in an color image to create 'template', looks through the whole image to detect pixels that are
 * similar to that template.
 * Example for section "Backprojecting a histogram to detect specific image content" in Chapter 4.
 */
object Ex7ContentDetectionColor extends App {

    // Load image as a color
    val colorImage = loadAndShowOrExit(new File("data/waves.jpg"), CV_LOAD_IMAGE_COLOR)

    // Reduce colors
    ColorHistogram.colorReduce(colorImage, 32)

    // Display image with marked ROI
    val rect = new Rectangle(0, 0, 165, 75)
    show(drawOnImage(colorImage, rect), "Input")

    // Define ROI for sample histogram
    colorImage.roi(toIplROI(rect))

    // Compute histogram within the ROI
    val hist = new ColorHistogram().getHistogram(colorImage)

    // Normalize histogram so the sum of all bins is equal to 1.
    cvNormalizeHist(hist, 1)

    // Remove ROI, we will be using full image for the rest
    colorImage.roi(null)

    // Prepare finder
    val finder = new ContentFinder
    finder.histogram = hist
    finder.threshold = 0.05f

    // Get back-projection of the color histogram
    val result = finder.find(colorImage)
    cvReleaseHist(hist)

    // Show results
    show(result, "Backprojection result. White means match, black no match.")
}
