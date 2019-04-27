/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter06

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * The example for section "Filtering images using low-pass filters" in Chapter 6, page 142.
 * Basic use of a Gaussian filter.
 */
object Ex1LowPassFilter extends App {

  // Read input image
  val src = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_GRAYSCALE)

  // Blur with a Gaussian filter
  //    val dest = cvCreateImage(cvGetSize(src), src.depth, 1)
  val dest = new Mat()
  val kernelSize = new Size(5, 5)
  val sigma = 1.5
  val borderType = BORDER_DEFAULT
  blur(src, dest, kernelSize)
  show(dest, "Blurred")
}
