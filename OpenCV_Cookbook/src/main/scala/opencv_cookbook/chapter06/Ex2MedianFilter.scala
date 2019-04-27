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
 * The example for section "Filtering images using a median filter" in Chapter 6, page 147.
 */
object Ex2MedianFilter extends App {

  // Read input image with a salt noise
  val src = loadAndShowOrExit(new File("data/boldt_salt.jpg"), IMREAD_GRAYSCALE)

  // Remove noise with a median filter
  val dest       = new Mat()
  val kernelSize = 3
  medianBlur(src, dest, kernelSize)
  show(dest, "Median filtered")

  // Since median filter really cleans up outlier with values above (salt) and below (pepper),
  // in this case, we can reconstruct dark pixels that are most likely not effected by the noise.
  val dest2 = new Mat()
  min(src, dest, dest2)
  show(dest2, "Median filtered + dark pixel recovery")
}
