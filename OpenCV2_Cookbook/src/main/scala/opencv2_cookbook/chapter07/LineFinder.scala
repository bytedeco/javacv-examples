/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter07


import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import scala.math._

/**
 * Helper class to detect lines segments using probabilistic Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 170.
 *
 * @see JavaCV sample [https://code.google.com/p/javacv/source/browse/javacv/samples/HoughLines.java]
 *
 * @param deltaRho Accumulator resolution distance.
 * @param deltaTheta  Accumulator resolution angle.
 * @param minVotes Minimum number of votes that a line must receive before being considered.
 * @param minLength  Minimum number of votes that a line must receive before being considered.
 * @param minGap Max gap allowed along the line. Default no gap.
 */
class LineFinder(val deltaRho: Double = 1,
                 val deltaTheta: Double = Pi / 180,
                 val minVotes: Int = 10,
                 val minLength: Double = 0,
                 val minGap: Double = 0d) {

    private var lines: CvSeq = null


    /**
     * Apply probabilistic Hough transform.
     */
    def findLines(binary: CvMat) {
        // Hough transform for line detection
        val storage = cvCreateMemStorage(0)
        lines = cvHoughLines2(binary, storage,
            CV_HOUGH_PROBABILISTIC, deltaRho, deltaTheta, minVotes, minLength, minGap)
        lines
    }


    /**
     * Draws detected lines on an image
     */
    def drawDetectedLines(image: IplImage) {
        for (i <- 0 until lines.total) {
            // from JavaCPP, the equivalent of the C code:
            // CvPoint* line = (CvPoint*)cvGetSeqElem(lines,i);
            // CvPoint first=line[0], second=line[1]
            // is:
            // CvPoint first=line.position(0), second=line.position(1);

            val line = cvGetSeqElem(lines, i)
            val pt1 = new CvPoint(line).position(0)
            val pt2 = new CvPoint(line).position(1)

            //            System.out.println("Line spotted: ")
            //            System.out.println("\t pt1: " + pt1)
            //            System.out.println("\t pt2: " + pt2)

            // draw the segment on the image
            cvLine(image, pt1, pt2, CV_RGB(255, 0, 0), 1, CV_AA, 0)
        }
    }

}
