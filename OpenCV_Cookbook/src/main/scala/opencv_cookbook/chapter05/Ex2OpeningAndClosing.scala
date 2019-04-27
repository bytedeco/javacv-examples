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
 * Example of using morphological opening and closing in chapter 5 section
 * "Opening and closing images using morphological filters".
 */
object Ex2OpeningAndClosing extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/binary.bmp"))

  // Create 5x5 structural element
  val element5 = new Mat(5, 5, CV_8U, new Scalar(1d))

  // Closing
  val closed = new Mat()
  morphologyEx(image, closed, MORPH_CLOSE, element5)
  show(closed, "Closed")

  // Opening
  val opened = new Mat()
  morphologyEx(image, opened, MORPH_OPEN, element5)
  show(opened, "Opened")
}