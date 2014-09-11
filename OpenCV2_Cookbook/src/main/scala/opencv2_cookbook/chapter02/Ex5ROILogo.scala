/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter02

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._


/**
 * Paste small image into a larger one using a region of interest. Mask is optional.
 *
 * Illustrates operation on a small portion of the input image: a region of interest (ROI).
 */
object Ex5ROILogo extends App {

  // Read input image
  val logo = loadIplAndShowOrExit(new File("data/logo.bmp"), CV_LOAD_IMAGE_COLOR)
  val mask = loadIplAndShowOrExit(new File("data/logo.bmp"), CV_LOAD_IMAGE_GRAYSCALE)
  val image = loadIplAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

  // Define region of interest that matches the size of the logo
  val roi = new IplROI()
  roi.xOffset(385)
  roi.yOffset(270)
  roi.width(logo.width)
  roi.height(logo.height)

  val imageROI = image.roi(roi)

  // Combine input image with the logo. Mask is used to control blending.
  cvCopy(logo, imageROI, mask)

  // Clear ROI after processing is done.
  // If ROI is not cleared further operations would apply to ROI only.
  // For instance, if saving the image, only part within the ROI would be saved.
  image.roi(null)

  // Display
  show(image, "With Logo")
}