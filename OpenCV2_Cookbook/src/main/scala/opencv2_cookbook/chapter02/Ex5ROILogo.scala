/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
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
  val logo = loadAndShowOrExit(new File("data/logo.bmp"), CV_LOAD_IMAGE_COLOR)
  val mask = loadOrExit(new File("data/logo.bmp"), CV_LOAD_IMAGE_GRAYSCALE)
  val image = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

  // Define region of interest that matches the size of the logo
  val imageROI = image(new Rect(image.cols - logo.cols, image.rows - logo.rows, logo.cols, logo.rows))

  // Combine input image with the logo. Mask is used to control blending.
  logo.copyTo(imageROI, mask)

  // Display
  show(image, "With Logo")
}