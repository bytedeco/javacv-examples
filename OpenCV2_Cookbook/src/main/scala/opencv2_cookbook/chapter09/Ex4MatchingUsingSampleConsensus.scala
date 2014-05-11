/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter09

import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_calib3d._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_features2d._
import org.bytedeco.javacpp.opencv_nonfree.SURF


/** The example for section "Matching images using random sample consensus" in Chapter 9, page 233.
  *
  * Most of the computations are done by `RobustMatcher` helper class.
  */
object Ex4MatchingUsingSampleConsensus extends App {

  // Read input images
  val image1 = loadMatOrExit(new File("data/canal1.jpg"))
  val image2 = loadMatOrExit(new File("data/canal2.jpg"))
  show(image1, "Right image")
  show(image2, "Left image")

  // Prepare the matcher
  val rMatcher = new RobustMatcher(
    confidenceLevel = 0.98,
    minDistanceToEpipolar = 1.0,
    ratio = 0.65F,
    detector = new SURF(10),
    refineF = true
  )

  //
  // Match two images
  //
  val matches = rMatcher.matchImages(image1, image2)

  // draw the matches
  val matchesCanvas = new Mat(image1.cols + image2.cols, image1.rows, CV_8UC3)
  val white = new Scalar(255, 255, 255, 0)
  val matchMask: Array[Byte] = null
  drawMatches(image1, matches.keyPoints1, image2, matches.keyPoints2,
    toNativeVector(matches.matches), matchesCanvas, white, Scalar.all(-1), matchMask, DrawMatchesFlags.DEFAULT)
  show(matchesCanvas, "Matches")

  // Draw the epipolar lines
  val (points1, points2) = MatcherUtils.toCvPoint2D32f(matches.matches, matches.keyPoints1, matches.keyPoints2)
  val lines1 = cvCreateMat(points1.capacity, 3, CV_32F)
  cvComputeCorrespondEpilines(toCvMat(points1), 1, matches.fundamentalMatrix, lines1)
  show(drawEpiLines(image2, lines1, points2), "Left Image Epilines (RANSAC)")
  val lines2 = cvCreateMat(points2.capacity, 3, CV_32F)
  cvComputeCorrespondEpilines(toCvMat(points2), 2, matches.fundamentalMatrix, lines2)
  show(drawEpiLines(image1, lines2, points1), "Right Image Epilines (RANSAC)")


  //----------------------------------------------------------------------------------------------------------------


  /** Draw `epilines` and `points` on a color copy of an `image`.
    *
    * @return new image  with epilines and points.
    */
  private def drawEpiLines(image: Mat, epilines: CvMat, points: CvPoint2D32f): Mat = {
    //        val canvas = cvCreateImage(cvGetSize(image), image.depth(), 3)
    //        cvCvtColor(image, canvas, CV_GRAY2BGR)
    val canvas = image.clone()
    val white = new Scalar(255, 255, 255, 0)
    val red = new Scalar(255, 0, 0, 0)
    for (i <- 0 until epilines.rows()) {
      // draw the epipolar line between first and last column
      val a = epilines.get(i, 0, 0)
      val b = epilines.get(i, 0, 1)
      val c = epilines.get(i, 0, 2)
      val x0 = 0
      val y0 = math.round(-(c + a * x0) / b).toInt
      val x1 = image.cols
      val y1 = math.round(-(c + a * x1) / b).toInt
      line(canvas, new Point(x0, y0), new Point(x1, y1), white, 1, CV_AA, 0)

      val xp = math.round(points.position(i).x())
      val yp = math.round(points.position(i).y())
      circle(canvas, new Point(xp, yp), 3, red, 1, CV_AA, 0)
    }
    points.position(0)
    canvas
  }
}
