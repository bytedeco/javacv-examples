/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter07

import opencv2_cookbook.OpenCVUtils._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File


/**
 * Detect lines segments using probabilistic Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 170.
 *
 * @see JavaCV sample [https://code.google.com/p/javacv/source/browse/javacv/samples/HoughLines.java]
 */
object Ex3HoughLineSegments extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/road.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    // Canny contours
    val contours = cvCreateImage(cvGetSize(src), src.depth(), 1)
    val threshold1 = 125
    val threshold2 = 350
    val apertureSize = 3
    cvCanny(src, contours, threshold1, threshold2, apertureSize)
    show(contours, "Canny Contours")

    // Set probabilistic Hough transform
    val finder = new LineFinder(minLength = 100, minGap = 20, minVotes = 80)

    finder.findLines(toCvMat(contours))

    // Draw lines on the canny contour image
    val colorDst = cvCreateImage(cvGetSize(src), src.depth(), 3)
    cvCvtColor(contours, colorDst, CV_GRAY2BGR)
    finder.drawDetectedLines(colorDst)
    show(colorDst, "Hough Line Segments")
}
