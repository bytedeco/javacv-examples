/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter08

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._


/**
 * The second example for section "Detecting Harris corners" in Chapter 8, page 194.
 *
 * Uses Harris Corner strength image to detect well localized corners,
 * replacing several closely located detections (blurred) by a single one.
 * Actual computations are implemented in class [[opencv_cookbook.chapter08.HarrisDetector]].
 */
object Ex2HarrisCornerDetector extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/church01.jpg"), IMREAD_GRAYSCALE)

  // Harris detector instance
  val harris = new HarrisDetector
  // Compute Harris values
  harris.detect(image)
  // Detect Harris corners
  val pts = harris.getCorners(0.01)

  // Draw Harris corners
  harris.drawOnImage(image, pts)
  show(image, "Harris Corners")
}
