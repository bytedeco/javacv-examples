/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.awt.Color
import java.awt.image.BufferedImage

import opencv_cookbook.OpenCVUtils.{wrapInIntPointer, wrapInMatVector}
import org.bytedeco.javacpp.indexer.FloatRawIndexer
import org.bytedeco.javacpp.{FloatPointer, IntPointer}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._


/**
  * Helper methods for performing histogram and look-up table operations, correspond to part of C++ class
  * Histogram1D in the OpenCV Cookbook sample code.
  */
object Histogram1D {

  /**
    * Apply a look-up table to an image.
    * It is a wrapper for OpenCV function `LUT`.
    *
    * @param image  input image
    * @param lookup look-up table
    * @return new image
    */
  def applyLookUp(image: Mat, lookup: Mat): Mat = {
    val dest = new Mat()
    LUT(image, lookup, dest)
    dest
  }

  /**
    * Equalize histogram of an image. The algorithm normalizes the brightness and increases the contrast of the image.
    * It is a wrapper for OpenCV function `equalizeHist`.
    *
    * @param src input image
    * @return new image
    */
  def equalize(src: Mat): Mat = {
    val dest = new Mat()
    equalizeHist(src, dest)
    dest
  }

}

/**
  * Helper class that simplifies usage of OpenCV `calcHist` function for single channel images.
  *
  * See OpenCV [[http://opencv.itseez.com/modules/imgproc/doc/histograms.html?highlight=histogram]]
  * documentation to learn backend details.
  */
class Histogram1D {

  private val numberOfBins = 256
  private val channels: IntPointer = wrapInIntPointer(0)
  private var _minRange = 0.0f
  private var _maxRange = 255.0f

  def setRanges(minRange: Float, maxRange: Float) {
    _minRange = minRange
    _maxRange = maxRange
  }

  def getHistogramImage(image: Mat): BufferedImage = {

    // Output image size
    val width = numberOfBins
    val height = numberOfBins

    val hist = getHistogramAsArray(image)
    // Set highest point to 90% of the number of bins
    val scale = 0.9 / hist.max * height

    // Create a color image to draw on
    val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val g = canvas.createGraphics()

    // Paint background
    g.setPaint(Color.WHITE)
    g.fillRect(0, 0, width, height)

    // Draw a vertical line for each bin
    g.setPaint(Color.BLUE)
    for (bin <- 0 until numberOfBins) {
      def h = math.round(hist(bin) * scale).toInt
      g.drawLine(bin, height - 1, bin, height - h - 1)
    }

    // Cleanup
    g.dispose()

    canvas
  }

  /**
    * Computes histogram of an image.
    *
    * @param image input image
    * @return histogram represented as an array
    */
  def getHistogramAsArray(image: Mat): Array[Float] = {
    // Create and calculate histogram object
    val hist = getHistogram(image)

    // Extract values to an array
    val dest = new Array[Float](numberOfBins)
    val histI = hist.createIndexer().asInstanceOf[FloatRawIndexer]
    for (bin <- 0 until numberOfBins) {
      dest(bin) = histI.get(bin)
    }

    dest
  }

  /**
    * Computes histogram of an image.
    *
    * @param image input image
    * @param mask  optional mask
    * @return OpenCV histogram object
    */
  def getHistogram(image: Mat, mask: Mat = new Mat()): Mat = {
    val histSize = wrapInIntPointer(numberOfBins)
    val ranges = new FloatPointer(_minRange, _maxRange)
    // Compute histogram
    val hist = new Mat()
    calcHist(wrapInMatVector(image), channels, mask, hist, histSize, ranges)
    hist
  }
}