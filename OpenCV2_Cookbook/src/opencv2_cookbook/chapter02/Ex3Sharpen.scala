/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter02

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Use kernel convolution to sharpen an image.
 */
object Ex3Sharpen extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

    // Define output image
    val dest = IplImage.create(cvGetSize(image), image.depth, 3)

    // Construct sharpening kernel, oll unassigned values are 0
    val kernel = CvMat.create(3, 3, CV_32F)
    kernel.put(1, 1, 5)
    kernel.put(0, 1, -1)
    kernel.put(2, 1, -1)
    kernel.put(1, 0, -1)
    kernel.put(1, 2, -1)

    // Filter the image
    filter2D(image, dest, -1, kernel, new CvPoint(-1, -1), 0, BORDER_DEFAULT)

    // Display
    show(dest, "Sharpened")
}