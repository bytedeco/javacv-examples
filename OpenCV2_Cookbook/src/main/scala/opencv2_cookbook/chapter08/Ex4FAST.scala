/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter08

import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_features2d.{KeyPoint, FastFeatureDetector}

/**
 * The example for section "Detecting FAST features" in Chapter 8, page 203.
 */
object Ex4FAST extends App {

  // Read input image
  val image = loadMatAndShowOrExit(new File("data/church01.jpg"))

  // Detect FAST features
  val ffd = new FastFeatureDetector(
    40 /* threshold for detection */ ,
    true /* non-max suppression */)
  val keyPoints = new KeyPoint()
  ffd.detect(image, keyPoints)

  // Draw keyPoints
  show(drawOnImage(image, keyPoints), "FAST Features")
}
