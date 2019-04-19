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
import org.bytedeco.opencv.opencv_xfeatures2d._


/**
 * Example of extracting SURF features from section "Detecting the scale-invariant SURF features" in chapter 8.
 */
object Ex5SURF extends App {


  // Read input image
  val image = loadAndShowOrExit(new File("data/church01.jpg"))

  // Detect SURF features.
  val keyPoints = new KeyPointVector()
  val hessianThreshold = 2500d
  val nOctaves = 4
  val nOctaveLayers = 2
  val extended = true
  val upright = false
  val surf = SURF.create(hessianThreshold, nOctaves, nOctaveLayers, extended, upright)
  surf.detect(image, keyPoints)

  // Draw keyPoints
  //    val featureImage = cvCreateImage(cvGetSize(image), image.depth(), 3)
  val featureImage = new Mat()
  drawKeypoints(image, keyPoints, featureImage, new Scalar(255, 255, 255, 0), DRAW_RICH_KEYPOINTS)
  show(featureImage, "SURF Features")
}