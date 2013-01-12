/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter09

import com.googlecode.javacv.cpp.opencv_calib3d._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_nonfree.SURF
import opencv2_cookbook.OpenCVUtils._
import opencv2_cookbook.chapter09.MatcherUtils._
import scala.collection.mutable.{ListBuffer, ArrayBuffer}


/** Robust matcher used by examples Ex3ComputeFundamentalMatrix and Ex5Homography.
  *
  * See Chapter 9 page 233.
  *
  * @param detector Feature point detector
  * @param extractor Feature descriptor extractor
  * @param ratio Max ratio between 1st and 2nd NN
  * @param refineF If `true` will refine the F matrix
  * @param minDistanceToEpipolar Min distance to epipolar
  * @param confidenceLevel Confidence level (probability)
  */
class RobustMatcher(detector: SURF = new SURF(100),
                    extractor: DescriptorExtractor = DescriptorExtractor.create("SURF"),
                    ratio: Float = 0.65f,
                    refineF: Boolean = true,
                    minDistanceToEpipolar: Double = 3.0,
                    confidenceLevel: Double = 0.99) {


    /** Holds results of matching images */
    case class Result(matches: Array[DMatch],
                      keyPoints1: KeyPoint,
                      keyPoints2: KeyPoint,
                      fundamentalMatrix: CvMat)


    /** Match feature points using symmetry test and RANSAC
      *
      * @return fundamental matrix.
      */
    def matchImages(image1: IplImage, image2: IplImage): Result = {

        // 1a. Detection of the SURF features
        val keyPoints1 = new KeyPoint()
        val keyPoints2 = new KeyPoint()
        detector.detect(image1, null, keyPoints1)
        detector.detect(image2, null, keyPoints2)
        println("Number of SURF points (1): " + keyPoints1.capacity())
        println("Number of SURF points (2): " + keyPoints2.capacity())

        // 1b. Extraction of the SURF descriptors
        val descriptors1 = new CvMat(null)
        val descriptors2 = new CvMat(null)
        extractor.compute(image1, keyPoints1, descriptors1)
        extractor.compute(image2, keyPoints2, descriptors2)
        println("descriptor matrix size: " + descriptors1.rows + " by " + descriptors1.cols)

        // 2. Match the two image descriptors

        // Construction of the matcher
        val matcher = new BFMatcher(NORM_L2)

        // from image 1 to image 2
        // based on k nearest neighbours (with k=2)
        val matches1 = new DMatchVectorVector()
        matcher.knnMatch(
            descriptors1, descriptors2,
            matches1, // vector of matches (up to 2 per entry)
            2, // return 2 nearest neighbours
            null, // mask
            false // compact result, used when mask != null
        )

        // from image 2 to image 1
        // based on k nearest neighbours (with k=2)
        val matches2 = new DMatchVectorVector()
        matcher.knnMatch(
            descriptors2, descriptors1,
            matches2, // vector of matches (up to 2 per entry)
            2, // return 2 nearest neighbours
            null, // mask
            false // compact result, used when mask != null
        )

        println("Number of matched points 1->2: " + matches1.size())
        println("Number of matched points 2->1: " + matches2.size())

        // 3. Remove matches for which NN ratio is > than threshold

        //  ... clean image 1 -> image 2 matches
        val matches1WithRatio = ratioTest(matches1)
        println("Number of matched points 1->2 with ratio: " + matches1WithRatio.size)
        //  ... clean image 2 -> image 1 matches
        val matches2WithRatio = ratioTest(matches2)
        println("Number of matched points 2->1 with ratio: " + matches2WithRatio.size)

        // 4. Remove non-symmetrical matches
        val symMatches = symmetryTest(matches1WithRatio, matches2WithRatio)
        println("Number of matched points (symmetry test): " + symMatches.length)

        // 5. Validate matches using RANSAC
        val (matches, fundamentalMatrix) = ransacTest(symMatches, keyPoints1, keyPoints2)

        Result(matches, keyPoints1, keyPoints2, fundamentalMatrix)
    }


    /** Filter matches for which NN ratio is > than threshold, also remove non-matches, if present in the input.
      *
      * @param matches collection of matches that will be filtered.
      * @return the number of removed points (corresponding entries being cleared, i.e. size will be 0)
      */
    private def ratioTest(matches: DMatchVectorVector): Array[(DMatch, DMatch)] = {

        // Find matches that need to be removed
        val destArray = ArrayBuffer[(DMatch, DMatch)]()
        for (i <- 0 until matches.size().toInt) {
            // if 2 NN has been identified
            if (matches.size(i) > 1) {
                if (matches.get(i, 0).distance / matches.get(i, 1).distance <= ratio) {
                    destArray.append((matches.get(i, 0), matches.get(i, 1)))
                }
            }
        }

        destArray.toArray
    }


    /** Insert symmetrical matches in returned array. */
    private def symmetryTest(matches1: Array[(DMatch, DMatch)], matches2: Array[(DMatch, DMatch)]): Array[DMatch] = {

        val destSeq = new ListBuffer[DMatch]()

        // For all matches image 1 -> image 2
        for (m1 <- matches1) {
            val m11 = m1._1
            var break = false
            for (m2 <- matches2; if (!break)) {
                val m21 = m2._1
                if (m11.queryIdx == m21.trainIdx && m21.queryIdx == m11.trainIdx) {
                    destSeq += new DMatch(m11.queryIdx, m11.trainIdx, m11.distance)
                    break = true
                }
            }
        }

        destSeq.toArray
    }


    /** Identify good matches using RANSAC
      *
      * @param matches  input matches
      * @return  surviving matches and the fundamental matrix
      */
    def ransacTest(matches: Array[DMatch], keyPoints1: KeyPoint, keyPoints2: KeyPoint): (Array[DMatch], CvMat) = {

        // Convert keypoints into Point2f
        val (points1, points2) = toCvPoint2D32f(matches, keyPoints1, keyPoints2)

        // Compute F matrix using RANSAC
        val fundamentalMatrix = CvMat.create(3, 3, CV_32F)
        val pointStatus = CvMat.create(matches.length, 1, CV_8U)
        cvFindFundamentalMat(
            toCvMat(points1) /*  points in first image */ ,
            toCvMat(points2) /*  points in second image */ ,
            fundamentalMatrix /* output */ ,
            CV_FM_RANSAC /* RANSAC method */ ,
            minDistanceToEpipolar, /* distance to epipolar plane */
            confidenceLevel /* confidence probability */ ,
            pointStatus /* match status (inlier ou outlier) */)

        // extract the surviving (inliers) matches
        val outMatches = new ListBuffer[DMatch]()
        for (i <- 0 until pointStatus.rows()) {
            val inlier = math.round(pointStatus.get(i)) != 0
            if (inlier) {
                outMatches += matches(i)
            }
        }

        println("Number of matched points (after cleaning): " + outMatches.length)

        if (refineF) {
            // The F matrix will be recomputed with all accepted matches
            val (points1, points2) = toCvPoint2D32f(outMatches.toArray, keyPoints1, keyPoints2)

            // Compute 8-point F from all accepted matches
            cvFindFundamentalMat(
                toCvMat(points1) /* points in first image */ ,
                toCvMat(points2) /* points in second image */ ,
                fundamentalMatrix /* output */ ,
                CV_FM_8POINT /* 8-point method */ ,
                minDistanceToEpipolar, /* distance to epipolar plane, only used with CV_FM_RANSAC */
                confidenceLevel /* confidence probability, only used with CV_FM_RANSAC */ ,
                null)
        }

        (outMatches.toArray, fundamentalMatrix)
    }

}
