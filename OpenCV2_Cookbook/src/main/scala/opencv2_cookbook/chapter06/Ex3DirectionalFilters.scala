/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter06


import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * The example for section "Applying directional filters to detect edges" in Chapter 6, page 148.
 */
object Ex3DirectionalFilters extends App {

    // Read input image with a salt noise
    val src = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

    val apertureSize = 3

    // Sobel edges in X
    val sobelX = IplImage.create(cvGetSize(src), IPL_DEPTH_32F, 1)
    cvSobel(src, sobelX, 1, 0, apertureSize)
    show(toIplImage8U(scaleTo01(sobelX)), "Sobel X")

    // Sobel edges in Y
    val sobelY = IplImage.create(cvGetSize(src), IPL_DEPTH_32F, 1)
    cvSobel(src, sobelY, 0, 1, apertureSize)
    show(toIplImage8U(scaleTo01(sobelY)), "Sobel Y")

    // Compute norm of directional images to create Sobel edge image
    val sobel = IplImage.create(cvGetSize(src), sobelX.depth(), sobelX.nChannels())
    cvAdd(abs(sobelX), abs(sobelY), sobel, null)
    show(toIplImage8U(scaleTo01(sobel)), "Sobel")

    //    val min = Array(Double.MaxValue)
    //    val max = Array(Double.MinValue)
    //    cvMinMaxLoc(sobel, min, max)
    //    println("Sobel min: " + min(0) + ", max: " + max(0) + ".")

    // Threshold edges
    val thresholded = IplImage.create(cvGetSize(sobel), IPL_DEPTH_8U, 1)
    cvThreshold(sobel, thresholded, 120, 255, CV_THRESH_BINARY_INV)
    show(thresholded, "Thresholded")


    /**
     * Helper for computing `cvAbs()` of an image.
     */
    def abs(src: IplImage): IplImage = {
        val dest = IplImage.create(cvGetSize(src), src.depth(), src.nChannels())
        cvAbs(src, dest)
        dest
    }
}
