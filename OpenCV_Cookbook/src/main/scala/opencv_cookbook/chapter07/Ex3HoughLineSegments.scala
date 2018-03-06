/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter07

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Detect lines segments using probabilistic Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 170.
 *
 * @see JavaCV sample [https://code.google.com/p/javacv/source/browse/javacv/samples/HoughLines.java]
 */
object Ex3HoughLineSegments extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/road.jpg"), IMREAD_GRAYSCALE)

    // Canny contours
    val contours = new Mat()
    val threshold1 = 125
    val threshold2 = 350
    val apertureSize = 3
    Canny(src, contours, threshold1, threshold2, apertureSize, false)
    show(contours, "Canny Contours")

    // Set probabilistic Hough transform
    val finder = new LineFinder(minLength = 100, minGap = 20, minVotes = 80)

    finder.findLines(contours)

    // Draw lines on the canny contour image
    val colorDst = new Mat()
    cvtColor(contours, colorDst, COLOR_GRAY2BGR)
    finder.drawDetectedLines(colorDst)
    show(colorDst, "Hough Line Segments")
}
