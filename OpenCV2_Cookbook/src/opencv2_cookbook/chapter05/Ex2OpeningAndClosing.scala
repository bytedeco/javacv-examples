/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter05

import java.io.File
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._
import opencv2_cookbook.OpenCVUtils._


/**
 * Example of using morphological opening and closing.
 * This Scala code functionally is equivalent to C++ code in chapter 5 section
 * "Opening and closing images using morphological filters".
 * The original example in the book is using "C++ API". Calls here use "C API" supported by JavaCV.
 */
object Ex2OpeningAndClosing extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/binary.bmp"))

    // Create 5x5 structural element
    val element5 = cvCreateStructuringElementEx(5, 5, 2, 2, CV_SHAPE_RECT, null)

    // Closing
    val closed = cvCreateImage(cvGetSize(image), image.depth, 1)
    cvMorphologyEx(image, closed, null, element5, MORPH_CLOSE, 1)
    show(closed, "Closed")

    // Opening
    val opened = cvCreateImage(cvGetSize(image), image.depth, 1)
    cvMorphologyEx(image, opened, null, element5, MORPH_OPEN, 1)
    show(opened, "Opened")
}