/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter07


import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar._
import org.bytedeco.javacpp.helper.{opencv_imgproc => imgproc}
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._
import scala.collection.mutable.ListBuffer


/**
 * The example for section "Computing components' shape descriptors" in Chapter 7, page 186.
 */
object Ex6ShapeDescriptors extends App {

  //
  // First part is the same as in example `Ex5ExtractContours`; extracts contours.
  //

  // Read input image
  val src = loadAndShowOrExit(new File("data/binaryGroup.bmp"), CV_LOAD_IMAGE_GRAYSCALE)

  // Extract connected components
  val contourSeq = new CvSeq(null)
  val storage = cvCreateMemStorage()
  imgproc.cvFindContours(src, storage, contourSeq, Loader.sizeof(classOf[CvContour]), CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE)

  // Convert to a Scala collection for easier manipulation
  val contours = toScalaSeq(contourSeq)

  // Draw extracted contours
  val colorDst = cvCreateImage(cvGetSize(src), src.depth(), 3)
  cvCvtColor(src, colorDst, CV_GRAY2BGR)
  drawAllContours(colorDst, contours)
  show(colorDst, "Contours")

  // Eliminate too short or too long contours
  val lengthMin = 100
  val lengthMax = 1000
  val filteredContours = contours.filter(contour => lengthMin < contour.total() && contour.total() < lengthMax)
  val colorDest2 = loadOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_COLOR)
  drawAllContours(colorDest2, filteredContours, width = 2)

  //
  // Second part computes shapes descriptors from the extracted contours.
  //

  // Testing the bounding box
  val update = 0
  val rectangle0 = cvBoundingRect(filteredContours(0), update)
  cvRectangleR(colorDest2, rectangle0, MAGENTA, 2, CV_AA, 0)

  // Testing the enclosing circle
  val center1 = Array(0f, 0f)
  val radius1 = Array(1f)
  cvMinEnclosingCircle(filteredContours(1), center1, radius1)
  cvCircle(colorDest2, cvPointFrom32f(center1), radius1(0).toInt, MAGENTA, 2, CV_AA, 0)

  // Testing the approximate polygon
  val poly2 = cvApproxPoly(filteredContours(2), Loader.sizeof(classOf[CvContour]), storage, CV_POLY_APPROX_DP, 5, 1)
  // Draw only the first poly
  cvDrawContours(colorDest2, poly2, MAGENTA, MAGENTA, -1, 2, CV_AA, cvPoint(0, 0))

  // Testing the convex hull
  val orientation = CV_CLOCKWISE
  val returnPoints = 1
  val convexHullPoints3 = cvConvexHull2(filteredContours(3), storage, CV_CLOCKWISE, returnPoints)
  drawAllContours(colorDest2, toScalaSeq(convexHullPoints3), MAGENTA, 2)

  // Testing the moments for all filtered contours, and marking center of mass on the image
  for (c <- filteredContours) {
    val moments = new CvMoments()
    cvMoments(c, moments, 0)
    val xCenter = math.round(moments.m10() / moments.m00).toInt
    val yCenter = math.round(moments.m01() / moments.m00).toInt
    cvCircle(colorDest2, cvPoint(xCenter, yCenter), 2, GREEN, 2, CV_AA, 0)
  }


  // Show the image with marked contours and shape descriptors
  show(colorDest2, "Contours Filtered")


  //------------------------------------------------------------------------


  /**
   * Convert OpenCV sequence to a Scala collection for easier handling.
   */
  def toScalaSeq(cvSeq: CvSeq): Seq[CvSeq] = {
    val seqBuilder = new ListBuffer[CvSeq]()
    var element = cvSeq
    while (element != null && !element.isNull) {
      if (element.elem_size() > 0) {
        seqBuilder += element
      }
      element = element.h_next()
    }
    seqBuilder.toSeq
  }


  /**
   * Draw `contours` on the `image`.
   */
  def drawAllContours(image: IplImage, contours: Seq[CvSeq], color: CvScalar = RED, width: Int = 1) {
    contours.foreach(cvDrawContours(image, _, color, color, -1, width, CV_AA, cvPoint(0, 0)))
  }
}
