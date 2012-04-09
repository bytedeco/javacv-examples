/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._
import java.io.File
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_core._


/**
 * The third example for section "Computing the image histogram" in Chapter 4, page 93.
 * Separates pixels in an image into a foreground (black) and background (white) using OpenCV `cvThreshold` method.
 */
object Ex3Threshold extends App {
    // Load image as a grayscale
    val src = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    val dest = cvCreateImage(cvGetSize(src), src.depth, src.nChannels)

    cvThreshold(src, dest, 60, 255, CV_THRESH_BINARY)

    show(dest, "Thresholded")
}