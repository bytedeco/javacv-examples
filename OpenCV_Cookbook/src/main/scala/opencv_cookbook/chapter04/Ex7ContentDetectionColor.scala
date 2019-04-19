/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.opencv_core._


/**
 * Uses histogram of region in an color image to create 'template'.
 * looks through the whole image to detect pixels that are  similar to that template.
 * Example for section "Backprojecting a histogram to detect specific image content" in Chapter 4.
 */
object Ex7ContentDetectionColor extends App {

  // Load image as a color
  val colorImage = loadAndShowOrExit(new File("data/waves.jpg"))

  // Blue sky area
  val rectROI = new Rect(0, 0, 100, 45)

  // Display image with marked ROI
  show(drawOnImage(colorImage, rectROI, new Scalar(0d, 255d, 255d, 0.5)), "Input")

  // Define ROI
  val imageROI = colorImage(rectROI)
  show(imageROI, "Reference")

  // Compute histogram within the ROI
  val hc = new ColorHistogram()
  hc.numberOfBins = 8
  val hist = hc.getHistogram(imageROI)

  val finder = new ContentFinder()
  finder.histogram = hist
  finder.threshold = 0.05f

  val result1 = finder.find(colorImage)
  show(result1, "Color Detection Result")

  // Second color image
  val colorImage2 = loadAndShowOrExit(new File("data/dog.jpg"))

  val result2 = finder.find(colorImage2)
  show(result2, "Color Detection Result 2")
}
