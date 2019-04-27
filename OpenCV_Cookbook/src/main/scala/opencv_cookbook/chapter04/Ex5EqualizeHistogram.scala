/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._

/**
 * Modifies image using histogram equalization.
 * Example for section "Equalizing the image histogram" in Chapter 4.
 */
object Ex5EqualizeHistogram extends App {

  // Load image as a gray scale
  val src = loadAndShowOrExit(new File("data/group.jpg"), IMREAD_GRAYSCALE)
  // Show histogram of the source image
  show(new Histogram1D().getHistogramImage(src), "Input histogram")

  // Apply look-up
  val dest = Histogram1D.equalize(src)

  // Show inverted image
  show(dest, "Equalized Histogram")
  // Show histogram of the modified image
  show(new Histogram1D().getHistogramImage(dest), "Equalized histogram")
}
