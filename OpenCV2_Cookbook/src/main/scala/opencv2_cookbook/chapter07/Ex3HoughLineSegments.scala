/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter07

import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


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

    finder.findLines(contours.asCvMat())

    // Draw lines on the canny contour image
    val colorDst = cvCreateImage(cvGetSize(src), src.depth(), 3)
    cvCvtColor(contours, colorDst, CV_GRAY2BGR)
    finder.drawDetectedLines(colorDst)
    show(colorDst, "Hough Line Segments")
}
