/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter10

import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

object MatcherUtils {

  /** Convert from KeyPoint to Point2D32f representation */
  def toPoint2fVectorPair(matches: DMatchVector, keyPoints1: KeyPointVector, keyPoints2: KeyPointVector): (Point2fVector, Point2fVector) = {

    // Extract keypoints from each match, separate Left and Right
    val size = matches.size.toInt
    val pointIndexes1 = new Array[Int](size)
    val pointIndexes2 = new Array[Int](size)
    for (i <- 0 until size) {
      pointIndexes1(i) = matches.get(i).queryIdx()
      pointIndexes2(i) = matches.get(i).trainIdx()
    }

    // Convert keypoints into Point2f
    val points1 = new Point2fVector()
    val points2 = new Point2fVector()
    KeyPoint.convert(keyPoints1, points1, pointIndexes1)
    KeyPoint.convert(keyPoints2, points2, pointIndexes2)

    (points1, points2)
  }

  def toDMatchVector(src: Seq[DMatch]): DMatchVector = {
    val dest = new DMatchVector(src.size)
    for ((m, i) <- src.toArray.zipWithIndex) {
      dest.put(i, m)
    }
    dest
  }

  def drawEpiLines(image: Mat, lines: Mat, points: Point2fVector): Mat = {
    val canvas = image.clone()
    val linesIndexer = lines.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- 0 until lines.rows()) {

      // draw the epipolar line between first and last column
      val a = linesIndexer.get(i, 0, 0)
      val b = linesIndexer.get(i, 0, 1)
      val c = linesIndexer.get(i, 0, 2)
      val x0 = 0
      val y0 = math.round(-(c + a * x0) / b)
      val x1 = image.cols
      val y1 = math.round(-(c + a * x1) / b)
      line(canvas, new Point(x0, y0), new Point(x1, y1), new Scalar(255, 255, 255, 0), 1, LINE_AA, 0)

      val xp = math.round(points.get(i).x)
      val yp = math.round(points.get(i).y)
      //        val (color, width) = if (inlier) (RED, 2) else (YELLOW, 1)
      val (color, width) = (new Scalar(0, 255, 255, 0), 1)
      circle(canvas, new Point(xp, yp), 3, color, width, LINE_AA, 0)
    }
    points.position(0)
    canvas
  }


}
