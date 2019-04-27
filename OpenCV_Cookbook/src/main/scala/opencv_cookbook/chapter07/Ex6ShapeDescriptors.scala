/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter07


import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._


/**
 * The example for section "Computing components' shape descriptors" in Chapter 7, page 186.
 */
object Ex6ShapeDescriptors extends App {

  val Red     = new Scalar(0, 0, 255, 0)
  val Magenta = new Scalar(255, 0, 255, 0)
  val Yellow  = new Scalar(0, 255, 255, 0)
  val Blue    = new Scalar(255, 0, 0, 0)
  val Cyan    = new Scalar(255, 255, 0, 0)
  val Green   = new Scalar(0, 255, 0, 0)

  //
  // First part is the same as in example `Ex5ExtractContours`; extracts contours.
  //

  // Read input image
  val src = loadAndShowOrExit(new File("data/binaryGroup.bmp"), IMREAD_GRAYSCALE)

  // Extract connected components
  val contourVec = new MatVector()
  val storage    = cvCreateMemStorage()
  findContours(src, contourVec, RETR_EXTERNAL, CHAIN_APPROX_NONE)

  // Draw extracted contours
  val colorDst = new Mat(src.size(), CV_8UC3, new Scalar(0))
  drawContours(colorDst, contourVec, -1 /* draw all contours */ , Red)
  show(colorDst, "Contours")

  // Convert to a Scala collection for easier manipulation/filtering
  val contours = toSeq(contourVec)

  // Eliminate too short or too long contours
  val lengthMin = 100
  val lengthMax = 1000
  val filteredContours = contours.filter(contour => lengthMin < contour.total() && contour.total() < lengthMax)
  val colorDest2 = loadOrExit(new File("data/group.jpg"), IMREAD_COLOR)
  drawContours(colorDest2, toMatVector(filteredContours), -1, Red, 2, LINE_8, noArray(), Int.MaxValue, new Point())

  //
  // Second part computes shapes descriptors from the extracted contours.
  //

  // Testing the bounding box
  //  val update = 0
  val rectangle0 = boundingRect(filteredContours.head)
  // Draw rectangle
  rectangle(colorDest2, rectangle0, Magenta, 2, LINE_AA, 0)

  // Testing the enclosing circle
  val center1 = new Point2f()
  val radius1 = new FloatPointer(1f)
  minEnclosingCircle(filteredContours(1), center1, radius1)
  // Draw circle
  circle(colorDest2, new Point(cvRound(center1.x), cvRound(center1.y)), radius1.get(0).toInt, Yellow, 2, LINE_AA, 0)

  // Testing the approximate polygon
  val poly2 = new Mat()
  approxPolyDP(filteredContours(2), poly2, 5, true)
  // Draw only the first poly.
  polylines(colorDest2, toMatVector(Seq(poly2)), true, Blue, 2, LINE_AA, 0)

  // Testing the convex hull
  val clockwise    = true
  val returnPoints = true
  val hull         = new Mat()
  convexHull(filteredContours(3), hull, clockwise, returnPoints)
  polylines(colorDest2, toMatVector(Seq(hull)), true, Cyan, 2, LINE_AA, 0)

  // Testing the moments for all filtered contours, and marking center of mass on the image
  for (c <- filteredContours) {
    val ms = moments(c)
    val xCenter = math.round(ms.m10() / ms.m00).toInt
    val yCenter = math.round(ms.m01() / ms.m00).toInt
    circle(colorDest2, new Point(xCenter, yCenter), 2, Green, 2, LINE_AA, 0)
  }

  // Show the image with marked contours and shape descriptors
  show(colorDest2, "Some Shape Descriptors")


  //------------------------------------------------------------------------


  /**
   * Convert to a Scala Seq collection.
   */
  private def toSeq(matVector: MatVector): Seq[Mat] =
    for (i <- 0 until matVector.size.toInt) yield matVector.get(i)

  /**
   * Convert Scala sequence to MatVector.
   */
  private def toMatVector(seq: Seq[Mat]): MatVector = new MatVector(seq: _*)
}
