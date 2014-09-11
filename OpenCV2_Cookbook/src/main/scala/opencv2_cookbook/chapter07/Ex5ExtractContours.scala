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
 * The example for section "Extracting the components' contours" in Chapter 7, page 182.
 */
object Ex5ExtractContours extends App {

    // Read input image
    val src = loadIplAndShowOrExit(new File("data/binaryGroup.bmp"), CV_LOAD_IMAGE_GRAYSCALE)

    // Extract connected components
    val contourSeq = new CvSeq(null)
  val storage = cvCreateMemStorage()
  imgproc.cvFindContours(src, storage, contourSeq, Loader.sizeof(classOf[CvContour]), CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE)

    // Convert to a Scala collection for easier manipulation
    val contours = toScalaSeq(contourSeq)

    // Draw extracted contours
    val colorDst = cvCreateImage(cvGetSize(src), src.depth(), 3)
    cvCvtColor(src, colorDst, CV_GRAY2BGR)
    draw(colorDst, contours)
    show(colorDst, "Contours")

    // Eliminate too short or too long contours
    val lengthMin = 100
    val lengthMax = 1000
    val filteredContours = contours.filter(contour => lengthMin < contour.total() && contour.total() < lengthMax)
    val colorDest2 = loadIplImageOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_COLOR)
    draw(colorDest2, filteredContours)
    show(colorDest2, "Contours Filtered")


    /**
     * Convert to a Scala Seq collection.
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
    def draw(image: IplImage, contours: Seq[CvSeq]) {
      contours.foreach(cvDrawContours(image, _, RED, RED, -1, 1, CV_AA, cvPoint(0, 0)))
    }
}
