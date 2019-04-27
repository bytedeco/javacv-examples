/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import org.bytedeco.javacpp.{FloatPointer, IntPointer, PointerPointer}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.{opencv_imgproc => imgproc}
import org.bytedeco.opencv.opencv_core._


/**
  * Used by examples from section "Backprojecting a histogram to detect specific image content" in chapter 4.
  */
class ContentFinder {
  private val _histogram = new Mat()
  private var _threshold = -1f

  /**
    * Find content back projecting a histogram.
    *
    * @param image input used for back projection.
    * @return Result of the back-projection of the histogram. Image is binary (0,255) if threshold is larger than 0.
    */
  def find(image: Mat): Mat = {

    val channels = Array(0, 1, 2)

    find(image, 0.0f, 255.0f, channels)
  }

  def find(image: Mat, minValue: Float, maxValue: Float, channels: Array[Int]): Mat = {
    val result = new Mat()

    // Create parameters that can be used for both 1D and 3D/color histograms.
    // Since C++ calcBackProject is using arrays of arrays we need to do some wrapping `PointerPointer` objects.
    val histRange = Array(minValue, maxValue)
    val intPtrChannels = new IntPointer(channels: _*)
    val ptrPtrHistRange = new PointerPointer[FloatPointer](histRange, histRange, histRange)

    calcBackProject(image, 1, intPtrChannels, histogram, result, ptrPtrHistRange, 255, true)

    if (threshold > 0)
      imgproc.threshold(result, result, 255 * threshold, 255, THRESH_BINARY)

    result
  }

  def threshold: Float = _threshold

  /**
    * Set threshold for converting the back-projected image to a binary.
    * If value is negative no thresholding will be done.
    */
  def threshold_=(t: Float) {
    _threshold = t
  }

  def histogram: Mat = _histogram

  /**
    * Set reference histogram, it will be normalized.
    */
  def histogram_=(h: Mat): Unit = {
    normalize(h, _histogram)
  }
}
