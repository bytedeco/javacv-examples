/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter02

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Use kernel convolution to sharpen an image.
 */
object Ex3Sharpen extends App {

  // Read input image
  val image = loadIplAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

  // Define output image
  val dest = cvCreateImage(cvGetSize(image), image.depth, 3)

  // Construct sharpening kernel, oll unassigned values are 0
  val kernel = cvCreateMat(3, 3, CV_32F)
  kernel.put(1, 1, 5)
  kernel.put(0, 1, -1)
  kernel.put(2, 1, -1)
  kernel.put(1, 0, -1)
  kernel.put(1, 2, -1)

  // Filter the image
  cvFilter2D(image, dest, kernel, cvPoint(-1, -1))

  // Display
  show(dest, "Sharpened")
}