/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter08

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * The first example for section "Detecting Harris corners" in Chapter 8, page 192.
 *
 * Computes Harris corners strength image and displays after applying a corner strength threshold.
 * In the output image strong corners are marked with black, background with white.
 */
object Ex1HarrisCornerMap extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/church01.jpg"), IMREAD_GRAYSCALE)

  // Image to store the Harris detector responses.
  val cornerStrength = new Mat()
  // Detect Harris Corners
  cornerHarris(image, cornerStrength,
    3 /* neighborhood size */ ,
    3 /* aperture size */ ,
    0.01 /* Harris parameter */)

  // Threshold to retain only locations of strongest corners
  val harrisCorners = new Mat()
  val t             = 0.0001
  threshold(cornerStrength, harrisCorners, t, 255, THRESH_BINARY_INV)
  // FIXME: `show` should work without converting to 8U
  show(toMat8U(harrisCorners), "Harris Corner Map")
}
