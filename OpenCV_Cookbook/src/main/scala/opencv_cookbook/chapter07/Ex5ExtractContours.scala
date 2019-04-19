/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter07

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._


/**
 * The example for section "Extracting the components' contours" in Chapter 7, page 182.
 */
object Ex5ExtractContours extends App {

  // Read input image
  val src = loadAndShowOrExit(new File("data/binaryGroup.bmp"), IMREAD_GRAYSCALE)

  // Extract connected components
  val contours = new MatVector()
  val hierarchy = new Mat()
  findContours(src, contours, RETR_EXTERNAL, CHAIN_APPROX_NONE, new Point(0, 0))

  val result = new Mat(src.size(), CV_8UC3, new Scalar(0))
  drawContours(result, contours,
    -1, // draw all contours
    new Scalar(0, 0, 255, 0))
  show(result, "Contours")

  // Eliminate too short or too long contours
  val lengthMin = 100
  val lengthMax = 1000
  val filteredContoursSeq = toSeq(contours).filter(contour =>
    lengthMin < contour.total() && contour.total() < lengthMax)

  val result2 = loadOrExit(new File("data/group.jpg"), IMREAD_COLOR)
  drawContours(result2, toMatVector(filteredContoursSeq),
    -1, // draw all contours
    new Scalar(0, 0, 255, 0))
  show(result2, "Contours Filtered")

  /**
   * Convert to a Scala Seq collection.
   */
  def toSeq(matVector: MatVector): Seq[Mat] =
    for (i <- 0 until matVector.size.toInt) yield matVector.get(i)

  /**
   * Convert Scala sequence to MatVector.
   */
  def toMatVector(seq: Seq[Mat]): MatVector = new MatVector(seq: _*)
}
