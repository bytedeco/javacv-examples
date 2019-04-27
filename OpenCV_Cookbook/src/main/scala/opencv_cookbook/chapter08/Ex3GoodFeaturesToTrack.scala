/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter08

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_features2d._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_features2d._

/**
 * Example of using the Good Features to Track detector.
 *
 * The third example for section "Detecting Harris corners" in Chapter 8, page 202.
 */
object Ex3GoodFeaturesToTrack extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/church01.jpg"))

  // Compute good features to track
  val gftt      = GFTTDetector.create(
    500 /* maximum number of corners to be returned */ ,
    0.01 /* quality level*/ ,
    10.0 /* minimum allowed distance between points*/ ,
    3 /* block size*/ ,
    false /* use Harris detector*/ ,
    0.04 /* Harris parameter */
  )
  val keyPoints = new KeyPointVector()
  gftt.detect(image, keyPoints)

  // Draw keyPoints
  val canvas = new Mat()
  drawKeypoints(image, keyPoints, canvas, new Scalar(255, 255, 255, 0), DEFAULT)
  show(canvas, "Good Features to Track Detector")
}