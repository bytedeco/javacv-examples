/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter08

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core.{KeyPointVector, Mat, Scalar}
import org.bytedeco.javacpp.opencv_features2d._


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
  drawKeypoints(image, keyPoints, canvas, new Scalar(255, 255, 255, 0), DrawMatchesFlags.DEFAULT)
  show(canvas, "Good Features to Track Detector")
}