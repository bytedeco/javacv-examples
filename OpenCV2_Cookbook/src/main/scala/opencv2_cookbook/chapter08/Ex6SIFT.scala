/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter08

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_features2d._
import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT


/**
 * Example of extracting SIFT features from section "Detecting the scale-invariant SURF features" in chapter 8.
 */
object Ex6SIFT extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/church01.jpg"))

  // Detect SIFT features.
  val keyPoints = new KeyPointVector()
  val nFeatures = 0
  val nOctaveLayers = 3
  val contrastThreshold = 0.03
  val edgeThreshold = 10
  val sigma = 1.6
  val sift = SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma)
  sift.detect(image, keyPoints)

  // Draw keyPoints
  val featureImage = new Mat()
  drawKeypoints(image, keyPoints, featureImage, new Scalar(255, 255, 255, 0), DrawMatchesFlags.DRAW_RICH_KEYPOINTS)
  show(featureImage, "SIFT Features")
}