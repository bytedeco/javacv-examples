/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter07


import org.bytedeco.javacpp.indexer.IntRawIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import scala.math._

/**
 * Helper class to detect lines segments using probabilistic Hough transform approach.
 * The example for section "Detecting lines in image with the Hough transform" in Chapter 7, page 170.
 *
 * @see JavaCV sample [https://code.google.com/p/javacv/source/browse/javacv/samples/HoughLines.java]
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

  private var lines: Mat = _


  /**
   * Apply probabilistic Hough transform.
   */
  def findLines(binary: Mat) {
    // Hough transform for line detection
    lines = new Mat()
    HoughLinesP(binary, lines, deltaRho, deltaTheta, minVotes, minLength, minGap)
  }


  /**
   * Draws detected lines on an image
   */
  def drawDetectedLines(image: Mat) {

    val indexer = lines.createIndexer().asInstanceOf[IntRawIndexer]

    for (i <- 0 until lines.rows()) {
      val pt1 = new Point(indexer.get(i, 0, 0), indexer.get(i, 0, 1))
      val pt2 = new Point(indexer.get(i, 0, 2), indexer.get(i, 0, 3))

      // draw the segment on the image
      line(image, pt1, pt2, new Scalar(0, 0, 255, 128), 1, LINE_AA, 0)
    }
  }

}
