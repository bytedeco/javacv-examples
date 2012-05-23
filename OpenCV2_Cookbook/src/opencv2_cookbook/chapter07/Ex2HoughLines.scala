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
import scala.math._


/**
 * Detect lines using standard Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 167.
 *
 * @see JavaCV sample [https://code.google.com/p/javacv/source/browse/javacv/samples/HoughLines.java]
 */
object Ex2HoughLines extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/road.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    // Canny contours
    val canny = cvCreateImage(cvGetSize(src), src.depth(), 1)
    val threshold1 = 125
    val threshold2 = 350
    val apertureSize = 3
    cvCanny(src, canny, threshold1, threshold2, apertureSize)
    show(canny, "Canny Contours")

    // Hough transform for line detection
    val storage = cvCreateMemStorage(0)
    val method = CV_HOUGH_STANDARD
    val distanceResolutionInPixels = 1
    val angleResolutionInRadians = Pi / 180
    val minimumVotes = 80
    val unused = 0
    val lines = cvHoughLines2(
        canny,
        storage,
        method,
        distanceResolutionInPixels,
        angleResolutionInRadians,
        minimumVotes,
        unused, unused)

    // Draw lines on the canny contour image
    val colorDst = cvCreateImage(cvGetSize(src), src.depth(), 3)
    cvCvtColor(canny, colorDst, CV_GRAY2BGR)
    for (i <- 0 until lines.total) {
        val point = new CvPoint2D32f(cvGetSeqElem(lines, i))
        val rho = point.x
        val theta = point.y
        val a = cos(theta)
        val b = sin(theta)
        val x0 = a * rho
        val y0 = b * rho
        val pt1 = new CvPoint(round(x0 + 1000 * (-b)).toInt, round(y0 + 1000 * (a)).toInt)
        val pt2 = new CvPoint(round(x0 - 1000 * (-b)).toInt, round(y0 - 1000 * (a)).toInt)

        cvLine(colorDst, pt1, pt2, CV_RGB(255, 0, 0), 1, CV_AA, 0)
    }
    show(colorDst, "Hough Lines")
}
