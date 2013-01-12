/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter07

import collection.mutable.ListBuffer
import com.googlecode.javacpp.Loader
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * The example for section "Extracting the components' contours" in Chapter 7, page 182.
 */
object Ex5ExtractContours extends App {

    // Read input image
    val src = loadAndShowOrExit(new File("data/binaryGroup.bmp"), CV_LOAD_IMAGE_GRAYSCALE)

    // Extract connected components
    val contourSeq = new CvSeq(null)
    val storage = CvMemStorage.create()
    cvFindContours(src, storage, contourSeq, Loader.sizeof(classOf[CvContour]), CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE)

    // Convert to a Scala collection for easier manipulation
    val contours = toScalaSeq(contourSeq)

    // Draw extracted contours
    val colorDst = IplImage.create(cvGetSize(src), src.depth(), 3)
    cvCvtColor(src, colorDst, CV_GRAY2BGR)
    draw(colorDst, contours)
    show(colorDst, "Contours")

    // Eliminate too short or too long contours
    val lengthMin = 100
    val lengthMax = 1000
    val filteredContours = contours.filter(contour => (lengthMin < contour.total() && contour.total() < lengthMax))
    val colorDest2 = loadOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_COLOR)
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
        contours.foreach(cvDrawContours(image, _, CvScalar.RED, CvScalar.RED, -1, 1, CV_AA))
    }
}
