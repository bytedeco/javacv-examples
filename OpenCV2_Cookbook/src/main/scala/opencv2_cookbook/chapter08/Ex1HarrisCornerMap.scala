/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter08

import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * The first example for section "Detecting Harris corners" in Chapter 8, page 192.
 *
 * Computes Harris corners strength image and displays after applying a corner strength threshold.
 * In the output image strong corners are marked with black, background with white.
 */
object Ex1HarrisCornerMap extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/church01.jpg"))

  // Image to store the Harris detector responses.
  val cornerStrength = cvCreateImage(cvGetSize(image), IPL_DEPTH_32F, 1)
  // Detect Harris Corners
  cvCornerHarris(image, cornerStrength,
    3 /* neighborhood size */ ,
    3 /* aperture size */ ,
    0.01 /* Harris parameter */)

  // Threshold to retain only locations of strongest corners
  val harrisCorners = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, 1)
  val threshold = 0.0001
  cvThreshold(cornerStrength, harrisCorners, threshold, 255, CV_THRESH_BINARY_INV)
  show(harrisCorners, "Harris Corner Map")
}
