/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter04

import com.googlecode.javacv.cpp.opencv_highgui._
import java.io.File
import opencv2_cookbook.OpenCVUtils._

/**
 * The second example for section "Computing the image histogram" in Chapter 4, page 92.
 * Displays a graph of a histogram created using utility class [[opencv2_cookbook.chapter04.Histogram1D]].
 */
object Ex2ComputeHistogramGraph extends App {

    // Load image as a grayscale since we will be calculating histogram of an image with a single channel
    val src = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    // Calculate histogram
    val h = new Histogram1D
    val histogram = h.getHistogramImage(src)

    // Display the graph
    show(histogram, "Histogram")
}