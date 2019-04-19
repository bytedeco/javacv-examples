/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter10

import java.io.File

import opencv_cookbook.OpenCVUtils._
import opencv_cookbook.chapter10.MatcherUtils._
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.opencv.global.opencv_calib3d._
import org.bytedeco.opencv.global.opencv_features2d._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_xfeatures2d._


/** The example for section "Matching images using random sample consensus" in Chapter 10, p. 299  (2nd edition)
  * and Chapter 9, page 233 (1st edition).
  *
  * Most of the computations are done by `RobustMatcher` helper class.
  */
object Ex4MatchingUsingSampleConsensus extends App {

  // Read input images
  val image1 = loadOrExit(new File("data/church01.jpg"))
  val image2 = loadOrExit(new File("data/church03.jpg"))
  show(image1, "Right image")
  show(image2, "Left image")

  // Prepare the matcher
  val rMatcher = new RobustMatcher(SURF.create())

  //
  // Match two images
  //
  val matches = rMatcher.matchImages(image1, image2, RobustMatcher.BothCheck)

  // draw the matches
  val imageMatches = new Mat()
  drawMatches(image1, matches.keyPoints1, // 1st image and its keypoints
    image2, matches.keyPoints2, // 2nd image and its keypoints
    toDMatchVector(matches.matches), // the matches
    imageMatches, // the image produced
    new Scalar(0, 0, 255, 0), // color of the lines
    new Scalar(255, 0, 0, 0), // color of the keypoints
    new BytePointer(0),
    2)

  show(imageMatches, "Matches")


  // Draw the epipolar lines
  val (points1, points2) = toPoint2fVectorPair(toDMatchVector(matches.matches), matches.keyPoints1, matches.keyPoints2)

  val lines1 = new Mat()
  computeCorrespondEpilines(toMat(points1), 1, matches.fundamentalMatrix, lines1)
  show(drawEpiLines(image2, lines1, points2), "Left Image Epilines (RANSAC)")
  val lines2 = new Mat()
  computeCorrespondEpilines(toMat(points2), 2, matches.fundamentalMatrix, lines2)
  show(drawEpiLines(image1, lines2, points1), "Right Image Epilines (RANSAC)")
}
