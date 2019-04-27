/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter05

import java.awt.geom.Ellipse2D
import java.awt.{Color, Graphics2D, Image}

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * Equivalent of C++ class MorphoFeatures presented in section "Detecting edges and filters using
 * morphological filters". Contains methods for morphological corner detection.
 */
class MorphoFeatures {

  // Threshold to produce binary image
  var thresholdValue: Int = -1

  // Structural elements used in corner detection
  private val cross   = new Mat(5, 5, CV_8U,
    new BytePointer(
      0, 0, 1, 0, 0,
      0, 0, 1, 0, 0,
      1, 1, 1, 1, 1,
      0, 0, 1, 0, 0,
      0, 0, 1, 0, 0
    )
  )
  private val diamond = new Mat(5, 5, CV_8U,
    new BytePointer(
      0, 0, 1, 0, 0,
      0, 1, 1, 1, 0,
      1, 1, 1, 1, 1,
      0, 1, 1, 1, 0,
      0, 0, 1, 0, 0
    )
  )
  private val square  = new Mat(5, 5, CV_8U,
    new BytePointer(
      1, 1, 1, 1, 1,
      1, 1, 1, 1, 1,
      1, 1, 1, 1, 1,
      1, 1, 1, 1, 1,
      1, 1, 1, 1, 1
    )
  )
  private val x       = new Mat(5, 5, CV_8U,
    new BytePointer(
      1, 0, 0, 0, 1,
      0, 1, 0, 1, 0,
      0, 0, 1, 0, 0,
      0, 1, 0, 1, 0,
      1, 0, 0, 0, 1
    )
  )


  def getEdges(image: Mat): Mat = {
    // Get gradient image
    val result = new Mat()
    morphologyEx(image, result, MORPH_GRADIENT, new Mat())

    // Apply threshold to obtain a binary image
    applyThreshold(result)

    result
  }


  def getCorners(image: Mat): Mat = {

    val result = new Mat()

    // Dilate with a cross
    dilate(image, result, cross)

    // Erode with a diamond
    erode(result, result, diamond)

    val result2 = new Mat()
    // Dilate with X
    dilate(image, result2, x)

    // Erode with a square
    erode(result2, result2, square)

    // Corners are obtained by differentiating the two closed images
    absdiff(result2, result, result)

    // Apply threshold to get binary image
    applyThreshold(result)

    result
  }


  private def applyThreshold(image: Mat) {
    if (thresholdValue > 0) {
      threshold(image, image, thresholdValue, 255, THRESH_BINARY_INV)
    }
  }


  /**
   * Draw circles at feature point locations on an image it assumes that images are of the same size.
   */
  def drawOnImage(binary: Mat, image: Mat): Image = {

    // OpenCV drawing seems to crash a lot, so use Java2D
    val binaryRaster = toBufferedImage(binary).getData
    val radius = 6
    val diameter = radius * 2

    val imageBI = toBufferedImage(image)
    val width = imageBI.getWidth
    val height = imageBI.getHeight
    val g2d = imageBI.getGraphics.asInstanceOf[Graphics2D]
    g2d.setColor(Color.WHITE)

    for (y <- 0 until height) {
      for (x <- 0 until width) {
        val v = binaryRaster.getSample(x, y, 0)
        if (v == 0) {
          g2d.draw(new Ellipse2D.Double(x - radius, y - radius, diameter, diameter))
        }
      }
    }

    imageBI
  }

}
