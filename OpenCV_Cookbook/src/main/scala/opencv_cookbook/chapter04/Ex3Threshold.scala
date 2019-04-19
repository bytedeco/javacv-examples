/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._


/**
 * The third example for section "Computing the image histogram" in Chapter 4, page 93.
 * Separates pixels in an image into a foreground (black) and background (white) using OpenCV `threshold` method.
 */
object Ex3Threshold extends App {

  // Load image as a gray scale
  val src = loadAndShowOrExit(new File("data/group.jpg"), IMREAD_GRAYSCALE)

  val dest = new Mat()

  threshold(src, dest, 60, 255, THRESH_BINARY)

  show(dest, "Thresholded")
}