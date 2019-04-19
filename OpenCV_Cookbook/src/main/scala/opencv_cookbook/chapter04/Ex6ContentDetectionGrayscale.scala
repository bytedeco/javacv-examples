/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._


/**
 * Uses histogram of a region in an gray scale image to create 'template',
 * looks through the whole image to detect pixels that are similar to that template.
 * Example for section "Backprojecting a histogram to detect specific image content" in Chapter 4.
 */
object Ex6ContentDetectionGrayscale extends App {

  // Load image as a gray scale
  val src = loadAndShowOrExit(new File("data/waves.jpg"), IMREAD_GRAYSCALE)

  val rectROI = new Rect(216, 33, 24, 30)

  // Display image with marked ROI
  show(drawOnImage(src, rectROI, new Scalar(1d, 255d, 255d, 0.5)), "Input")

  // Define ROI
  val imageROI = src(rectROI)
  show(imageROI, "Reference")

  // Compute histogram within the ROI
  val h = new Histogram1D()
  val hist = h.getHistogram(imageROI)
  show(h.getHistogramImage(imageROI), "Reference Histogram")

  val finder = new ContentFinder()
  finder.histogram = hist

  val result1 = finder.find(src)
  val tmp = new Mat()
  result1.convertTo(tmp, CV_8U, -1, 255)
  show(tmp, "Back-projection result")

  finder.threshold = 0.12f
  val result2 = finder.find(src)
  show(result2, "Detection result")
}
