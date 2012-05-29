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
 * Detect circles using Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 175.
 */
object Ex4HoughCircles extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/chariot.jpg"), CV_LOAD_IMAGE_GRAYSCALE)


    // Blur with a Gaussian filter
    val smooth = cvCreateImage(cvGetSize(src), src.depth, 1)
    val kernelSize = new CvSize(5, 5)
    val sigma = 1.5
    val borderType = BORDER_DEFAULT
    GaussianBlur(src, smooth, kernelSize, sigma, sigma, borderType)
    show(smooth, "Blured")


    // Compute Hough Circle transform
    val storage = cvCreateMemStorage(0)
    // accumulator resolution (size of the image / 2)
    val dp = 2
    // minimum distance between two circles
    val minDist = 50
    // Canny high threshold
    val highThreshold = 200
    // minimum number of votes
    val votes = 100
    val minRadius = 25
    val maxRadius = 100
    val circles = cvHoughCircles(smooth, storage, CV_HOUGH_GRADIENT,
        dp, minDist, highThreshold, votes, minRadius, maxRadius)


    // Draw lines on the canny contour image
    val colorDst = cvCreateImage(cvGetSize(src), src.depth(), 3)
    cvCvtColor(src, colorDst, CV_GRAY2BGR)
    for (i <- 0 until circles.total) {
        val point = new CvPoint3D32f(cvGetSeqElem(circles, i))
        val center = cvPointFrom32f(new CvPoint2D32f(point.x, point.y))
        val radius = math.round(point.z)
        cvCircle(colorDst, center, radius, CV_RGB(255, 0, 0), 1, CV_AA, 0)
        print(point)
    }
    show(colorDst, "Hough Circles")
}
