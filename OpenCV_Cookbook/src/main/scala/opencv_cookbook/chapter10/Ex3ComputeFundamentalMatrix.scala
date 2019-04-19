/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter10

import java.io.File
import java.nio.ByteBuffer

import opencv_cookbook.OpenCVUtils._
import opencv_cookbook.chapter10.MatcherUtils._
import org.bytedeco.opencv.global.opencv_calib3d._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_features2d._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_features2d._
import org.bytedeco.opencv.opencv_xfeatures2d._


/** The example for section "Computing the fundamental matrix of an image pair" in Chapter 9, page 228.
  *
  * The example first loads two images, one is considered "right" view, the other "left" view.
  * SURF features are computed for each image.
  * Features are matched to find corresponding points.
  * The fundamental matrix is computed using the "7 Point" approaches.
  * (using half of the corresponding points, that is about 130).
  * For each the fundamental matrix the epilines are computed and visualized on the input images.
  */
object Ex3ComputeFundamentalMatrix extends App {

  // Read input images
  val imageRight = loadAndShowOrExit(new File("data/church01.jpg"))
  val imageLeft  = loadAndShowOrExit(new File("data/church03.jpg"))

  // Construction of the SURF feature detector
  val surf = SURF.create(3000, 4, 2, false, false)

  // Detection of the SURF features
  val keypointsRight = new KeyPointVector()
  val keypointsLeft  = new KeyPointVector()
  surf.detect(imageRight, keypointsRight)
  surf.detect(imageLeft, keypointsLeft)

  println("Number of SURF points (Right): " + keypointsRight.size)
  println("Number of SURF points (Left): " + keypointsLeft.size)

  // Draw the keypoints
  drawKeyPoints(imageRight, keypointsRight, "Right SURF Features")
  drawKeyPoints(imageLeft, keypointsLeft, "Left SURF Features")

  // Extraction of the SURF descriptors
  val descriptorsRight = new Mat()
  val descriptorsLeft  = new Mat()
  surf.compute(imageRight, keypointsRight, descriptorsRight)
  surf.compute(imageLeft, keypointsLeft, descriptorsLeft)

  println("descriptor matrix size (Right): " + descriptorsRight.rows + " by " + descriptorsRight.cols)
  println("descriptor matrix size (Left) : " + descriptorsLeft.rows + " by " + descriptorsLeft.cols)

  // Construction of the BruteForce L2 matcher
  val matcher = new BFMatcher(NORM_L2, true)

  // Match the two image descriptors
  val matches = new DMatchVector()
  // "match" is a keyword in Scala, to avoid conflict between a keyword and a method match of the BFMatcher,
  // we need to enclose method name in ticks: `match`.
  matcher.`match`(descriptorsRight, descriptorsLeft, matches)

  println("Number of matched points: " + matches.size)

  // Select few matches, use similar approach as in [[opencv_cookbook.chapter08.Ex7DescribingSURF]]
  val selectedMatches = selectBest(matches, 7)

  // Use 7-Point method
  fundamentalMatrix7Point()

  //----------------------------------------------------------------------------------------------------------------


  /** Compute fundamental matrix using 7-Point method. */
  def fundamentalMatrix7Point() {
    // Select 7 points that match well and are spread around the image. This is done by manually inspecting the points.
    //    val selected7Matches = new DMatchVector(7)
    val selected7Matches = selectedMatches

    // Draw selected matches
    val blue = new Scalar(0, 0, 255, 0)
    val red = new Scalar(255, 0, 0, 0)
    val matchMask: ByteBuffer = null
    val imageMatches = new Mat()
    drawMatches(imageRight, keypointsRight, imageLeft, keypointsLeft,
      selected7Matches, imageMatches, blue, red, matchMask, DEFAULT)
    show(imageMatches, "Matches 7-point")


    // Extract keypoints from each match, separate Left and Right
    val pointIndexesRight = new Array[Int](selected7Matches.size().toInt)
    val pointIndexesLeft = new Array[Int](selected7Matches.size().toInt)
    for (i <- 0 until selected7Matches.size().toInt) {
      pointIndexesRight(i) = selected7Matches.get(i).queryIdx()
      pointIndexesLeft(i) = selected7Matches.get(i).trainIdx()
    }

    // Convert keypoints into Point2f
    val selPointsRight = new Point2fVector()
    val selPointsLeft = new Point2fVector()
    KeyPoint.convert(keypointsRight, selPointsRight, pointIndexesRight)
    KeyPoint.convert(keypointsLeft, selPointsLeft, pointIndexesLeft)

    // Check by drawing the points
    show(drawOnImage(imageRight, selPointsRight), "selPointsRight")
    show(drawOnImage(imageLeft, selPointsLeft), "selPointsLeft")

    // Convert to CvMat as needed by `cvFindFundamentalMat`
    val selPointsMatRight = toMat(selPointsRight)
    val selPointsMatLeft = toMat(selPointsLeft)

    // Compute F matrix from 7 matches
    // The 7-point method of computing the fundamental matrix may return up to 3 solutions, so use 9x3 matrix
    val fundamentalMatrix = findFundamentalMat(
      selPointsMatRight /* points in first image */ ,
      selPointsMatLeft /* points in second image */ ,
      FM_7POINT /* 7-point method */ ,
      3, 0.99, null /* additional parameters with default values, not used by the 7-point method */)
    println("F-Matrix size= " + fundamentalMatrix.rows + "," + fundamentalMatrix.cols)

    // Select the first solution
    val fundamentalMatrix1 = new Mat(fundamentalMatrix, new Rect(0, 0, 3, 3))

    // Draw the left points corresponding epipolar lines in right image
    val lines1 = new Mat()
    computeCorrespondEpilines(
      selPointsMatRight,
      1 /* in image 1 (can also be 2) */ ,
      fundamentalMatrix1 /* F matrix */ ,
      lines1 /* epipolar lines */)
    show(drawEpiLines(imageLeft, lines1, selPointsLeft), "Left Image Epilines (RANSAC)")

    // Draw the right points corresponding epipolar lines in left image
    val lines2 = new Mat()
    computeCorrespondEpilines(selPointsMatLeft, 2, fundamentalMatrix1, lines2)
    show(drawEpiLines(imageRight, lines2, selPointsRight), "Right Image Epilines (RANSAC)")
  }


  private def drawKeyPoints(image: Mat, keypoints: KeyPointVector, title: String) {
    //        val canvas = cvCreateImage(cvGetSize(image), image.depth(), 3)
    val canvas = new Mat()
    val white = new Scalar(255, 255, 255, 0)
    drawKeypoints(image, keypoints, canvas, white, DRAW_RICH_KEYPOINTS)
    show(canvas, title)
  }


  /** Select only the best matches from the list. Return new list. */
  private def selectBest(matches: DMatchVector, numberToSelect: Int): DMatchVector = {
    // Convert to Scala collection, and sort
    val sorted = toArray(matches).sortWith(_ lessThan _)

    // Select the best, and return in native vector
    toVector(sorted.take(numberToSelect))
  }
}
