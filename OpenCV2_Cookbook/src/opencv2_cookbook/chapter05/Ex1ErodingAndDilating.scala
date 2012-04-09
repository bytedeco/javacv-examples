/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter05


import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_core._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Example of using morphological erosion and dilation.
 * This Scala code functionally is equivalent to C++ code in chapter 5 section
 * "Eroding and dilating images using morphological filters".
 * The original example in the book is using "C++ API". Calls here use "C API" supported by JavaCV.
 */
object Ex1ErodingAndDilating extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/binary.bmp"))

    // Erode the image, by default 3x3 element is used
    val eroded = cvCreateImage(cvGetSize(image), image.depth, 1)
    cvErode(image, eroded, null, 1)
    show(eroded, "Eroded")

    // Dilate image, by default 3x3 element is used
    val dilated = cvCreateImage(cvGetSize(image), image.depth, 1)
    cvDilate(image, dilated, null, 1)
    show(dilated, "Dilated")

    // Erode with 7x7 structural element
    // First define rectangular kernel of size 7x7 with anchor point located in the middle, offset=3.
    val kernelSize = 7;
    val KernelAnchorOffset = 3;
    val kernel = cvCreateStructuringElementEx(
        kernelSize, kernelSize,
        KernelAnchorOffset, KernelAnchorOffset,
        CV_SHAPE_RECT, null)
    val eroded7x7 = cvCreateImage(cvGetSize(image), image.depth, 1)
    cvErode(image, eroded7x7, kernel, 1)
    show(eroded7x7, "Eroded 7x7")

    // Erode with 7x7 structural element
    // You can do it using 3x3 kernel and iterating 3 times.
    // Note: iterating 2 times will give 5x5 kernel equivalent, iterating 4 times will get 9x9, ...
    val eroded3x3i3 = cvCreateImage(cvGetSize(image), image.depth, 1)
    cvErode(image, eroded3x3i3, null, 3)
    show(eroded3x3i3, "Eroded 3x3, 3 times (effectivly 7x7)")
}