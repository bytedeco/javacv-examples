/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter04

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * The third example for section "Computing the image histogram" in Chapter 4, page 93.
 * Separates pixels in an image into a foreground (black) and background (white) using OpenCV `threshold` method.
 */
object Ex3Threshold extends App {

  // Load image as a gray scale
  val src = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

  val dest = new Mat()

  threshold(src, dest, 60, 255, CV_THRESH_BINARY)

  show(dest, "Thresholded")
}