/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._

import scala.math.round

/**
 * The first example for section "Computing the image histogram" in Chapter 4, page 91.
 * Computes histogram using utility class [[opencv_cookbook.chapter04.Histogram1D]] and prints values to the screen.
 */
object Ex1ComputeHistogram extends App {

  // Load image as a gray scale since we will be calculating histogram of an image with a single channel
  val src = loadAndShowOrExit(new File("data/group.jpg"), IMREAD_GRAYSCALE)

  // Calculate histogram
  val h = new Histogram1D
  val histogram = h.getHistogramAsArray(src)

  // Print histogram values, the Scala way
  histogram.zipWithIndex.foreach {
    case (count, bin) => println("" + bin + ": " + round(count))
  }

  // Validate histogram computations
  val numberOfPixels = src.cols * src.rows
  println("Number of pixels     : " + numberOfPixels)
  val sumOfHistogramBins = round(histogram.sum)
  println("Sum of histogram bins: " + sumOfHistogramBins)
  require(numberOfPixels == sumOfHistogramBins, "Number of pixels must be equal the sum of histogram bins")
}