/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter04

import java.awt.Rectangle
import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.helper.{opencv_imgproc => imgproc}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Uses histogram of a region in an gray scale image to create 'template',
 * looks through the whole image to detect pixels that are similar to that template.
 * Example for section "Backprojecting a histogram to detect specific image content" in Chapter 4.
 */
object Ex6ContentDetectionGrayscale extends App {

  // Load image as a gray scale
  val src = loadOrExit(new File("data/waves.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

  // Display image with marked ROI
  val rect = new Rectangle(360, 44, 40, 50)
  show(drawOnImage(src, rect), "Input")

  // Define ROI
  src.roi(toIplROI(rect))

  // Compute histogram within the ROI
  val h = new Histogram1D().getHistogram(src)

  // Normalize histogram so the sum of all bins is equal to 1.
  cvNormalizeHist(h, 1)

  // Remove ROI, we will be using full image for the rest
  src.roi(null)

  // Back projection is done using 32 floating point copy of the input image.
  // The output is also 32 bit floating point
  val dest = cvCreateImage(cvGetSize(src), IPL_DEPTH_32F, src.nChannels)
  imgproc.cvCalcBackProject(Array(toIplImage32F(src)), dest, h)
  cvReleaseHist(h)

  // Show results
  show(scaleTo01(dest), "Backprojection result")
}
