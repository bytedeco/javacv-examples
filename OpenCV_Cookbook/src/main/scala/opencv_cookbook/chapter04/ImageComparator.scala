/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._


/**
 * Computes image similarity using `compareHist`.
 */
class ImageComparator(val referenceImage: Mat, val numberOfBins: Int = 8) {

  private val hist = new ColorHistogram()
  hist.numberOfBins = numberOfBins

  private val referenceHistogram = hist.getHistogram(referenceImage)


  /**
   * Compare the reference image with the given input image and return similarity score.
   */
  def compare(image: Mat): Double = {
    val inputH = hist.getHistogram(image)
    compareHist(referenceHistogram, inputH, HISTCMP_INTERSECT)
  }
}
