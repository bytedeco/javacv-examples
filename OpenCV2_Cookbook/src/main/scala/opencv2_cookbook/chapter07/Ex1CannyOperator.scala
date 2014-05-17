/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter07

import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * The example for section "Detecting image contours with the Canny operator" in Chapter 7, page 164.
 */
object Ex1CannyOperator extends App {

  // Read input image
  val src = loadAndShowOrExit(new File("data/road.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

  // Canny contours
  val contours = cvCreateImage(cvGetSize(src), src.depth(), 1)
  val threshold1 = 125
  val threshold2 = 350
  val apertureSize = 3
  cvCanny(src, contours, threshold1, threshold2, apertureSize)

  show(contours, "Canny Contours")
}
