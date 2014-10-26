/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter05

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Example of using morphological opening and closing.
 * This Scala code functionally is equivalent to C++ code in chapter 5 section
 * "Opening and closing images using morphological filters".
 * The original example in the book is using "C++ API". Calls here use "C API" supported by JavaCV.
 */
object Ex2OpeningAndClosing extends App {

  // Read input image
  val image = loadIplAndShowOrExit(new File("data/binary.bmp"))

  // Create 5x5 structural element
  val values: Array[Int] = null
  val element5 = cvCreateStructuringElementEx(5, 5, 2, 2, CV_SHAPE_RECT, values)

  // Closing
  val closed = cvCreateImage(cvGetSize(image), image.depth, 1)
  cvMorphologyEx(image, closed, null, element5, MORPH_CLOSE, 1)
  show(closed, "Closed")

  // Opening
  val opened = cvCreateImage(cvGetSize(image), image.depth, 1)
  cvMorphologyEx(image, opened, null, element5, MORPH_OPEN, 1)
  show(opened, "Opened")
}