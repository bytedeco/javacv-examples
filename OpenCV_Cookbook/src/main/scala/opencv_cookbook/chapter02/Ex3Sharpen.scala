/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter02

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._


/**
 * Use kernel convolution to sharpen an image.
 */
object Ex3Sharpen extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_COLOR)

  // Define output image
  val dest = new Mat()

  // Construct sharpening kernel, oll unassigned values are 0
  val kernel = new Mat(3, 3, CV_32F, new Scalar(0))
  // Indexer is used to access value in the matrix
  val ki = kernel.createIndexer().asInstanceOf[FloatIndexer]
  ki.put(1, 1, 5)
  ki.put(0, 1, -1)
  ki.put(2, 1, -1)
  ki.put(1, 0, -1)
  ki.put(1, 2, -1)

  // Filter the image
  filter2D(image, dest, image.depth(), kernel)

  // Display
  show(dest, "Sharpened")
}