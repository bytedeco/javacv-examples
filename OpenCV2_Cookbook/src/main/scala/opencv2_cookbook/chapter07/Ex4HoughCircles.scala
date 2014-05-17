/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter07

import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Detect circles using Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 175.
 */
object Ex4HoughCircles extends App {

  // Read input image
  val src = loadMatAndShowOrExit(new File("data/chariot.jpg"), CV_LOAD_IMAGE_GRAYSCALE)


  // Blur with a Gaussian filter
  val smooth = new Mat()
  val kernelSize = new Size(5, 5)
  val sigma = 1.5
  val borderType = BORDER_DEFAULT
  GaussianBlur(src, smooth, kernelSize, sigma, sigma, borderType)
  show(smooth, "Blurred")


  // Compute Hough Circle transform
  val storage = cvCreateMemStorage(0)
  // accumulator resolution (size of the image / 2)
  val dp = 2
  // minimum distance between two circles
  val minDist = 50
  // Canny high threshold
  val highThreshold = 200
  // minimum number of votes
  val votes = 100
  val minRadius = 25
  val maxRadius = 100
  val circles = cvHoughCircles(smooth.asIplImage(), storage, CV_HOUGH_GRADIENT,
    dp, minDist, highThreshold, votes, minRadius, maxRadius)


  // Draw lines on the canny contour image
  val srcIpl = src.asIplImage()
  val colorDst = cvCreateImage(cvGetSize(srcIpl), srcIpl.depth(), 3)
  cvCvtColor(srcIpl, colorDst, CV_GRAY2BGR)
  for (i <- 0 until circles.total) {
    val point = new CvPoint3D32f(cvGetSeqElem(circles, i))
    val center = Array[Int](math.round(point.x), math.round(point.y))
    val radius = math.round(point.z)
    cvCircle(colorDst, center, radius, CV_RGB(255, 0, 0), 1, CV_AA, 0)
  }
  show(colorDst, "Hough Circles")
}
