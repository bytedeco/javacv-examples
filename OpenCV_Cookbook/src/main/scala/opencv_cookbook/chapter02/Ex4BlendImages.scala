/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter02

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._


/**
 * Blend two images using weighted addition.
 *
 * This example demonstrates image arithmetic, in particular weighted addition that is very useful for image blending.
 */
object Ex4BlendImages extends App {

  // Read input images
  val image1 = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_COLOR)
  val image2 = loadAndShowOrExit(new File("data/rain.jpg"), IMREAD_COLOR)

  // Define output image
  val result = new Mat()

  // Create blended image
  addWeighted(image1, 0.7, image2, 0.9, 0.0, result)

  // Display
  show(result, "Blended")
}