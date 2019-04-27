/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter07

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_imgproc._


/**
 * Detect circles using Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 175.
 */
object Ex4HoughCircles extends App {

  // Read input image
  val src = loadAndShowOrExit(new File("data/chariot.jpg"), IMREAD_GRAYSCALE)


  // Blur with a Gaussian filter
  val smooth = new Mat()
  val kernelSize = new Size(5, 5)
  val sigma = 1.5
  val borderType = BORDER_DEFAULT
  GaussianBlur(src, smooth, kernelSize, sigma, sigma, borderType)
  show(smooth, "Blurred")


  // Compute Hough Circle transform
  // accumulator resolution (size of the image / 2)
  val dp      = 2
  // minimum distance between two circles
  val minDist = 33
  // Canny high threshold
  val highThreshold = 200
  // minimum number of votes
  val votes     = 100
  val minRadius = 40
  val maxRadius = 90
  val circles = new Vec3fVector()
  HoughCircles(smooth, circles, HOUGH_GRADIENT, dp, minDist, highThreshold, votes, minRadius, maxRadius)
  // Draw lines on the canny contour image
  val colorDst = new Mat()
  cvtColor(src, colorDst, COLOR_GRAY2BGR)
  for (i <- 0 until circles.size().toInt) {
    val c = circles.get(i)
    val center = new Point(cvRound(c.get(0)), cvRound(c.get(1)))
    val radius = cvRound(c.get(2))
    println(s"Circle ((${center.x}, ${center.y}), $radius)")
    circle(colorDst, center, radius, new Scalar(0, 0, 255, 0), 1, LINE_AA, 0)
  }
  show(colorDst, "Hough Circles")
}
