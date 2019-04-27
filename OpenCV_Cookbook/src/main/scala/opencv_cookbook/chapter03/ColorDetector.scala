/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter03

import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.opencv_core._

import scala.math.abs


/** Example of using a strategy pattern in algorithm design.
  *
  * The pattern encapsulates an algorithm into a separate class.
  * To run this example use [[opencv_cookbook.chapter03.Ex1ColorDetector]].
  *
  * The algorithm converts the input image to a binary by checking is pixel color is within a given distance from
  * a desired color.
  * Pixels with color close to the desired color are white, other black.
  *
  * This Scala code functionally is equivalent to C++ code in chapter 3 section
  * "Using the Strategy pattern in algorithm design".
  * The original example in the book is using "C++ API".
  * Here we use JavaCPP Indexer to access pixel values in the image.
  *
  * Unlike the in the C++ example, this class does not pre-allocates and hold space for process image,
  * it is create only when needed.
  */
class ColorDetector(private var _minDist: Int = 100,
                    private var _target: ColorRGB = ColorRGB(130, 190, 230)) {

  def colorDistanceThreshold: Int = _minDist

  def colorDistanceThreshold_=(dist: Int) {
    _minDist = scala.math.max(0, dist)
  }

  def targetColor: ColorRGB = _target

  def targetColor_=(color: ColorRGB) {
    _target = color
  }

  def process(image: Mat): Mat = {

    // Indexer for input image
    val srcI = image.createIndexer().asInstanceOf[UByteIndexer]

    // Create output image and itx indexer
    val dest = new Mat(image.rows, image.cols, CV_8U)
    val destI = dest.createIndexer().asInstanceOf[UByteIndexer]

    // Iterate through pixels and check if their distance from the target color is
    // withing the distance threshold, if it is set `dest` to 255.
    val brg = new Array[Int](3)
    for (y <- 0 until image.rows) {
      for (x <- 0 until image.cols) {
        srcI.get(y, x, brg)
        val c = ColorRGB.fromBGR(brg)
        val t = if (distance(c) < colorDistanceThreshold) (255 & 0xFF).toByte else 0.toByte
        // Convert indexes to Long to avoid API ambiguity
        destI.put(y.toLong, x.toLong, t)
      }
    }

    dest
  }

  private def distance(color: ColorRGB): Double = {
    abs(targetColor.red - color.red) +
      abs(targetColor.green - color.green) +
      abs(targetColor.blue - color.blue)
  }
}