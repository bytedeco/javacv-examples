/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter06


import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * Computation of Laplacian and zero-crossing.
 * Helper class for section "Computing the Laplacian of an image" in Chapter 6, page 156,
 * used in `Ex4Laplacian`.
 */
class LaplacianZC {

  /**
   * Aperture size of the Laplacian kernel
   */
  var aperture = 5

  /**
   * Compute floating point Laplacian.
   */
  def computeLaplacian(src: Mat): Mat = {
    val laplace = new Mat()
    Laplacian(src, laplace, CV_32F, aperture, 1 /*scale*/ , 0 /*delta*/ , BORDER_DEFAULT)
    laplace
  }

  /**
   * Get binary image of the zero-crossings
   * if the product of the two adjustment pixels is
   * less than threshold then this is a zero crossing
   * will be ignored.
   */
  def getZeroCrossings(laplace: Mat): Mat = {

    // Threshold at 0
    val signImage = new Mat()
    threshold(laplace, signImage, 0, 255, THRESH_BINARY)

    // Convert the +/- image into CV_8U
    val binary = new Mat()
    signImage.convertTo(binary, CV_8U)

    // Dilate the binary image +/- regions
    val dilated = new Mat()
    dilate(binary, dilated, new Mat())

    // Return the zero-crossing contours
    val dest = new Mat()
    subtract(dilated, binary, dest)
    dest
  }

}
