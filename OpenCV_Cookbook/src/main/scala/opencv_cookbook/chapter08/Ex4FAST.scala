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
 * The example for section "Detecting FAST features" in Chapter 8, page 203.
 */
object Ex4FAST extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/church01.jpg"))

  // Detect FAST features
  val ffd = FastFeatureDetector.create(
    40 /* threshold for detection */ ,
    true /* non-max suppression */ ,
    FastFeatureDetector.TYPE_9_16)
  val keyPoints = new KeyPointVector()
  ffd.detect(image, keyPoints)

  // Draw keyPoints
  val canvas = new Mat()
  drawKeypoints(image, keyPoints, canvas, new Scalar(255, 255, 255, 0), DEFAULT)
  show(canvas, "FAST Features")
}
