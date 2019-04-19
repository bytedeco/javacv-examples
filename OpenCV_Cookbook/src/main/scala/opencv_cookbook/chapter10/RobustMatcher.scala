/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter10

import opencv_cookbook.OpenCVUtils._
import opencv_cookbook.chapter10.MatcherUtils._
import opencv_cookbook.chapter10.RobustMatcher._
import org.bytedeco.javacpp.indexer.{FloatIndexer, UByteRawIndexer}
import org.bytedeco.opencv.global.opencv_calib3d._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_features2d._
import org.bytedeco.opencv.opencv_xfeatures2d._

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object RobustMatcher {

  // Define possible values for CrossCheckType type, in type safe manner
  case object NoCheck extends CrossCheckType
  case object CrossCheck extends CrossCheckType
  case object RatioCheck extends CrossCheckType
  case object BothCheck extends CrossCheckType
  sealed trait CrossCheckType
}


/** Robust matcher used by examples Ex4MatchingUsingSampleConsensus.
  *
  * See  Chapter 10, p. 299  (2nd edition) or Chapter 9, page 233 (1st edition).
  *
  * @param feature2D             Feature point detector/extractor
  * @param ratio                 Max ratio between 1st and 2nd NN
  * @param refineF               If `true` will refine the F matrix
  * @param refineM               if `true` will refine the matches (will refine F also)
  * @param minDistanceToEpipolar Min distance to epipolar
  * @param confidenceLevel       Confidence level (probability)
  */
class RobustMatcher(feature2D: Feature2D = SURF.create(),
                    ratio: Float = 0.65f,
                    refineF: Boolean = true,
                    refineM: Boolean = true,
                    minDistanceToEpipolar: Double = 3.0,
                    confidenceLevel: Double = 0.99) {

  val normType: Int = NORM_L2

  /** Holds results of matching images */
  case class Result(matches: Array[DMatch],
                    keyPoints1: KeyPointVector,
                    keyPoints2: KeyPointVector,
                    fundamentalMatrix: Mat)

  /** Match feature points using symmetry test and RANSAC
    *
    * @return fundamental matrix.
    */
  def matchImages(image1: Mat, image2: Mat, crossCheckType: CrossCheckType = CrossCheck): Result = {

    // 1. Detection of the feature points
    val keyPoints1 = new KeyPointVector()
    val keyPoints2 = new KeyPointVector()
    feature2D.detect(image1, keyPoints1, new Mat())
    feature2D.detect(image2, keyPoints2, new Mat())
    println("Number of feature points (1): " + keyPoints1.size())
    println("Number of feature points (2): " + keyPoints2.size())

    // 2. Extraction of the feature descriptors
    val descriptors1 = new Mat()
    val descriptors2 = new Mat()
    feature2D.compute(image1, keyPoints1, descriptors1)
    feature2D.compute(image2, keyPoints2, descriptors2)
    println("descriptor matrix size: " + descriptors1.rows + " by " + descriptors1.cols)

    // 3. Match the two image descriptors
    //    (optionally apply some checking method)

    // Construction of the matcher with crosscheck
    val matcher = new BFMatcher(normType, crossCheckType == CrossCheck)

    // vectors of matches
    val matches1 = new DMatchVectorVector()
    val matches2 = new DMatchVectorVector()

    if (crossCheckType == RatioCheck || crossCheckType == BothCheck) {
      // from image 1 to image 2
      // based on k nearest neighbours (with k=2)
      matcher.knnMatch(descriptors1, descriptors2,
        matches1, // vector of matches (up to 2 per entry)
        2); // return 2 nearest neighbours

      println("Number of matched points 1->2: " + matches1.size)

      if (crossCheckType == BothCheck) {
        // from image 2 to image 1
        // based on k nearest neighbours (with k=2)
        matcher.knnMatch(descriptors2, descriptors1,
          matches2, // vector of matches (up to 2 per entry)
          2); // return 2 nearest neighbours

        println("Number of matched points 2->1: " + matches2.size)
      }
    }


    // select check method
    val outputMatches: DMatchVector = crossCheckType match {
      case CrossCheck =>
        val r = new DMatchVector()
        matcher.`match`(descriptors1, descriptors2, r)
        println("Number of matched points 1->2 (after cross-check): " + r.size)
        r
      case RatioCheck =>
        val r = ratioTest(matches1)
        println("Number of matched points 1->2 (after ratio test): " + r.length)
        toDMatchVector(r)
      case BothCheck =>
        val r = ratioAndSymmetryTest(matches1, matches2)
        println("Number of matched points 1->2 (after ratio and cross-check): " + r.length)
        toDMatchVector(r)
      case NoCheck =>
        val r = new DMatchVector()
        matcher.`match`(descriptors1, descriptors2, r)
        println("Number of matched points 1->2: " + r.size)
        r
    }

    // 4. Validate matches using RANSAC
    val (refinedMatches, fundamentalMatrix) = ransacTest(outputMatches, keyPoints1, keyPoints2)
    println("Number of matched points (after RANSAC): " + refinedMatches.length)

    Result(refinedMatches, keyPoints1, keyPoints2, fundamentalMatrix)
  }


  /**
    * Filter matches for which NN ratio is > than threshold, also remove non-matches, if present in the input.
    *
    * @param matches collection of matches that will be filtered.
    * @return the number of removed points (corresponding entries being cleared, i.e. size will be 0)
    */
  private def ratioTest(matches: DMatchVectorVector): Array[DMatch] = {

    // Find matches that need to be removed
    val destArray = ArrayBuffer[DMatch]()
    for (i <- 0 until matches.size().toInt) {
      val aMatch = matches.get(i)
      // if 2 NN has been identified
      if (aMatch.size() > 1) {
        if (aMatch.get(0).distance / aMatch.get(1).distance <= ratio) {
          destArray.append(aMatch.get(0))
        }
      }
    }

    destArray.toArray
  }

  /** Insert symmetrical matches in returned array. */
  private def symmetryTest(matches1: Array[DMatch], matches2: Array[DMatch]): Array[DMatch] = {

    val destSeq = new ListBuffer[DMatch]()

    // For all matches image 1 -> image 2
    for (m1 <- matches1) {
      var break = false
      for (m2 <- matches2; if !break) {
        if (m1.queryIdx == m2.trainIdx && m2.queryIdx == m1.trainIdx) {
          destSeq += new DMatch(m1.queryIdx, m1.trainIdx, m1.distance)
          break = true
        }
      }
    }

    destSeq.toArray
  }

  /*
  Apply both ratio and symmetry test
   * (often an over-kill)
   */
  def ratioAndSymmetryTest(matches1: DMatchVectorVector,
                           matches2: DMatchVectorVector): Array[DMatch] = {

    // Remove matches for which NN ratio is > than threshold

    // clean image 1 -> image 2 matches
    val ratioMatches1 = ratioTest(matches1)
    println("Number of matched points 1->2 (ratio test) : " + ratioMatches1.length)

    // clean image 2 -> image 1 matches
    val ratioMatches2 = ratioTest(matches2)
    println("Number of matched points 1->2 (ratio test) : " + ratioMatches2.length)

    // Remove non-symmetrical matches
    val outputMatches = symmetryTest(ratioMatches1, ratioMatches2)

    println("Number of matched points (symmetry test): " + outputMatches.length)
    outputMatches
  }


  /** Identify good matches using RANSAC
    *
    * @param srcMatches input matches
    * @return surviving matches and the fundamental matrix
    */
  def ransacTest(srcMatches: DMatchVector, keyPoints1: KeyPointVector, keyPoints2: KeyPointVector): (Array[DMatch], Mat) = {

    val (refinedMatches1, fundamentalMatrix) = {

      // Convert keypoints into Point2f
      val (points1, points2) = toPoint2fVectorPair(srcMatches, keyPoints1, keyPoints2)

      // Compute F matrix using RANSAC
      val pointStatus = new Mat()
      val fundamentalMatrix = findFundamentalMat(
        toMat(points1) /*  points in first image */ ,
        toMat(points2) /*  points in second image */ ,
        pointStatus /* match status (inlier or outlier) */ ,
        FM_RANSAC /* RANSAC method */ ,
        minDistanceToEpipolar, /* distance to epipolar plane */
        confidenceLevel /* confidence probability */
      )

      // extract the surviving (inliers) matches
      val outMatches = new ListBuffer[DMatch]()
      val pointStatusIndexer = pointStatus.createIndexer().asInstanceOf[UByteRawIndexer]
      for (i <- 0 until pointStatus.rows()) {
        val inlier = pointStatusIndexer.get(i) != 0
        if (inlier) {
          outMatches += srcMatches.get(i)
        }
      }
      (outMatches, fundamentalMatrix)
    }

    println("Number of matched points (after cleaning): " + refinedMatches1.length)

    if (refineF || refineM) {
      // The F matrix will be recomputed with all accepted matches
      val (points1, points2) = toPoint2fVectorPair(toDMatchVector(refinedMatches1), keyPoints1, keyPoints2)

      // Compute 8-point F from all accepted matches
      val fundamentalMatrix = findFundamentalMat(
        toMat(points1) /* points in first image */ ,
        toMat(points2) /* points in second image */ ,
        FM_8POINT /* 8-point method */ ,
        minDistanceToEpipolar, /* distance to epipolar plane, only used with FM_RANSAC */
        confidenceLevel /* confidence probability, only used with FM_RANSAC */ ,
        null)

      if (refineM) {
        val newPoints1 = new Mat()
        val newPoints2 = new Mat()
        // refine the matches
        correctMatches(fundamentalMatrix, // F matrix
          toMat(points1), toMat(points2), // original position
          newPoints1, newPoints2)
        printInfo(newPoints1, "newPoints1")
        printInfo(newPoints2, "newPoints2")
        // new position
        val newPoints1Indexer = newPoints1.createIndexer().asInstanceOf[FloatIndexer]
        val newPoints2Indexer = newPoints2.createIndexer().asInstanceOf[FloatIndexer]
        for (i <- 0 until points1.size.toInt) {
          println("i: " + i)
          val newPoint1x = newPoints1Indexer.get(0, i, 0)
          val newPoint1y = newPoints1Indexer.get(0, i, 1)
          val newPoint2x = newPoints2Indexer.get(0, i, 0)
          val newPoint2y = newPoints2Indexer.get(0, i, 1)
          println("(" +
            keyPoints1.get(refinedMatches1(i).queryIdx).pt.x + "," +
            keyPoints1.get(refinedMatches1(i).queryIdx).pt.y +
            ") -> (" + newPoint1x + "," + newPoint1y + ")")
          println("(" +
            keyPoints2.get(refinedMatches1(i).trainIdx).pt.x + "," +
            keyPoints2.get(refinedMatches1(i).trainIdx).pt.y +
            ") -> (" + newPoint2x + "," + newPoint2y + ")")

          keyPoints1.get(refinedMatches1(i).queryIdx).pt.x(newPoint1x)
          keyPoints1.get(refinedMatches1(i).queryIdx).pt.y(newPoint1y)
          keyPoints2.get(refinedMatches1(i).trainIdx).pt.x(newPoint2x)
          keyPoints2.get(refinedMatches1(i).trainIdx).pt.y(newPoint2y)
        }
      }

      (refinedMatches1.toArray, fundamentalMatrix)
    } else {
      (refinedMatches1.toArray, fundamentalMatrix)
    }
  }

}
