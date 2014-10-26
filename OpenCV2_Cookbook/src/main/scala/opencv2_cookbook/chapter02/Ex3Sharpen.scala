/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter02

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Use kernel convolution to sharpen an image.
 */
object Ex3Sharpen extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

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