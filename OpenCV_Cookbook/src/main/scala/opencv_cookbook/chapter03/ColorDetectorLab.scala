/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter03

import org.bytedeco.javacpp.indexer.{UByteIndexer, UByteRawIndexer}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

import scala.math.abs


/**
 * Example of using a strategy pattern in algorithm design.
 * The pattern encapsulates an algorithm into a separate class.
 * To run this example use [[opencv_cookbook.chapter03.Ex1ColorDetector]].
 *
 * The algorithm converts the input image to a binary by checking is pixel color is within a given distance from a desired color.
 * Pixels with color close to the desired color are white, other black.
 * Image is first converted from RGB to L*a*b* color space. Distance is computed in L*a*b*.
 *
 * This Scala code functionally is equivalent to C++ code in chapter 3 section
 * "Converting color spaces".
 * To make operations on image pixels easier and more efficient
 * OpenCV color image is converted to ImageJ representation during processing.
 *
 * Unlike the in the C++ example, this class does not pre-allocates and hold space for process image,
 * it is create only when needed.
 */
class ColorDetectorLab(private var _minDist: Int = 30,
                       // Need to remember that Color is interpreted here as L*a*b* scaled to (0-255), rather than RGB
                       // It as also stored as (b*, a*, L*)
                       private var _targetLab: ColorLab = ColorLab(74, -9, -26)) {


  def colorDistanceThreshold: Int = _minDist

  def colorDistanceThreshold_=(dist: Int) {
    _minDist = math.max(0, dist)
  }

  def targetColor: ColorLab = _targetLab

  def targetColor_=(color: ColorLab) {
    _targetLab = color
  }

  def process(rgbImage: Mat): Mat = {

    // Convert input from RGB to L*a*b* color space
    // Note that since destination image uses 8 bit unsigned integers, original L*a*b* values
    // are converted to fit 0-255 range
    //       L <- L*255/100
    //       a <- a + 128
    //       b <- b + 128
    val labImage = new Mat()
    cvtColor(rgbImage, labImage, COLOR_BGR2Lab)

    val indexer = labImage.createIndexer().asInstanceOf[UByteRawIndexer]

    // Create output image
    val dest = new Mat(labImage.rows, labImage.cols, CV_8U)
    val destIndexer = dest.createIndexer().asInstanceOf[UByteIndexer]

    // Iterate through pixels and check if their distance from the target color is
    // withing the distance threshold, if it is set `dest` to 255
    for (r <- 0 until labImage.rows) {
      for (c <- 0 until labImage.cols) {
        // Need to remember that now Color is interpreted as L*a*b* scaled to (0-255), rather than RGB
        // though distance calculation here work the same as for RGB
        val v = if (distance(colorAt(indexer, r, c)) < _minDist) 255.toByte else 0.toByte
        // Convert indexes to Long to avoid API ambiguity
        destIndexer.put(r.toLong, c.toLong, v)
      }
    }

    dest
  }

  case class Triple(l: Int, a: Int, b: Int)

  private def colorAt(indexer: UByteRawIndexer, c: Int, r: Int): Triple = {
    Triple(indexer.get(c, r, 0), indexer.get(c, r, 1), indexer.get(c, r, 2))
  }

  private def distance(color: Triple): Double = {
    // When converting to 8-bit representation L* is scaled, a* and b* are only shifted.
    // To make the distance calculations more proportional we scale here L* difference back.
    abs(_targetLab.lAsUInt8 - color.l) / 255d * 100d +
      abs(_targetLab.aAsUInt8 - color.a) +
      abs(_targetLab.bAsUInt8 - color.b)
  }
}