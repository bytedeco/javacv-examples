/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter06


import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * The example for section "Applying directional filters to detect edges" in Chapter 6, page 148.
 */
object Ex3DirectionalFilters extends App {

  // Read input image with a salt noise
  val src = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_GRAYSCALE)

  // Sobel edges in X
  val sobelX = new Mat()
  Sobel(src, sobelX, CV_32F, 1, 0)
  show(toMat8U(sobelX), "Sobel X")

  // Sobel edges in Y
  val sobelY = new Mat()
  Sobel(src, sobelY, CV_32F, 0, 1)
  show(toMat8U(sobelY), "Sobel Y")

  // Compute norm of directional images to create Sobel edge image
  val sobel = sobelX.clone()
  magnitude(sobelX, sobelY, sobel)
  show(toMat8U(sobel), "Sobel1")

  val min = new DoublePointer(1)
  val max = new DoublePointer(1)
  minMaxLoc(sobel, min, max, null, null, new Mat())
  println("Sobel min: " + min.get(0) + ", max: " + max.get(0) + ".")

  // Threshold edges
  // Prepare image for display: extract foreground
  val thresholded = new Mat()
  threshold(sobel, thresholded, 100, 255, THRESH_BINARY_INV)

  // FIXME: There us a crash if trying to display directly
  //   Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: 16711426
  //	   at java.awt.image.ComponentColorModel.getRGBComponent(ComponentColorModel.java:903)
  //  show(thresholded, "Thresholded")
  //  save(new File("Ex3DirectionalFilters-thresholded.tif"), thresholded)
  show(toMat8U(thresholded), "Thresholded")
}
