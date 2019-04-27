/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter05

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * Example of using morphological erosion and dilation in chapter 5 section
 * "Eroding and dilating images using morphological filters".
 */
object Ex1ErodingAndDilating extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/binary.bmp"))

  // Erode the image, by default 3x3 element is used
  val eroded = new Mat()
  erode(image, eroded, new Mat())
  show(eroded, "Eroded")

  // Dilate image, by default 3x3 element is used
  val dilated = new Mat()
  dilate(image, dilated, new Mat())
  show(dilated, "Dilated")

  // Erode with 7x7 structural element
  // First define rectangular kernel of size 7x7.
  val eroded7x7 = new Mat()
  // Note that scalar argument is Double meaning that is is an initial value, value of Int would mean size.
  val element   = new Mat(7, 7, CV_8U, new Scalar(1d))
  erode(image, eroded7x7, element)
  show(eroded7x7, "Eroded 7x7")

  // Erode with 7x7 structural element
  // You can do it using 3x3 kernel and iterating 3 times.
  // Note: iterating 2 times will give 5x5 kernel equivalent, iterating 4 times will get 9x9, ...
  val eroded3x3i3 = new Mat()
  erode(image, eroded3x3i3, new Mat(), new Point(-1, -1), 3, BORDER_CONSTANT, morphologyDefaultBorderValue)
  show(eroded3x3i3, "Eroded 3x3, 3 times (effectively 7x7)")
}