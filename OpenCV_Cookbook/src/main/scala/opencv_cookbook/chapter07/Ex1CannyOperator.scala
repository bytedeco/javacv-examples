/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter07

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._


/**
 * The example for section "Detecting image contours with the Canny operator" in Chapter 7, page 164.
 */
object Ex1CannyOperator extends App {

  // Read input image
  val src = loadAndShowOrExit(new File("data/road.jpg"), IMREAD_GRAYSCALE)

  // Canny contours
  val contours = new Mat()
  val threshold1 = 125
  val threshold2 = 350
  val apertureSize = 3
  Canny(src, contours, threshold1, threshold2, apertureSize, true /*L2 gradient*/)

  show(contours, "Canny Contours")
}
