/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter09

import com.googlecode.javacv.cpp.opencv_calib3d._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_nonfree._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/** The example for section "Computing the fundamental matrix of an image pair" in Chapter 9, page 228.
  *
  * The example first loads two images, one is considered "right" view, the other "left" view.
  * SURF features are computed for each image.
  * Features are matched to find corresponding points.
  * The fundamental matrix is computed from corresponding points using two approaches: "7 Point" and RANSAC
  * (using half of the corresponding points, that is about 130).
  * For each the fundamental matrix the epilines are computed and visualized on the input images.
  */
object Ex3ComputeFundamentalMatrix extends App {

    // Read input images
    val imageRight = loadAndShowOrExit(new File("data/church01.jpg"))
    val imageLeft = loadAndShowOrExit(new File("data/church03.jpg"))

    // Construction of the SURF feature detector
    val surf = new SURF(3000)

    // Detection of the SURF features
    val keypointsRight = new KeyPoint()
    val keypointsLeft = new KeyPoint()
    surf.detect(imageRight, null, keypointsRight)
    surf.detect(imageLeft, null, keypointsLeft)

    println("Number of SURF points (Right): " + keypointsRight.capacity)
    println("Number of SURF points (Left): " + keypointsLeft.capacity)

    // Draw the keypoints
    drawKeyPoints(imageRight, keypointsRight, "Right SURF Features")
    drawKeyPoints(imageLeft, keypointsLeft, "Left SURF Features")

    // Construction of the SURF descriptor extractor
    val surfDesc = DescriptorExtractor.create("SURF")

    // Extraction of the SURF descriptors
    val descriptorsRight = new CvMat(null)
    val descriptorsLeft = new CvMat(null)
    surfDesc.compute(imageRight, keypointsRight, descriptorsRight)
    surfDesc.compute(imageLeft, keypointsLeft, descriptorsLeft)

    println("descriptor matrix size (Right): " + descriptorsRight.rows + " by " + descriptorsRight.cols)
    println("descriptor matrix size (Left) : " + descriptorsLeft.rows + " by " + descriptorsLeft.cols)

    // Construction of the BruteForce L2 matcher
    val matcher = new BFMatcher(NORM_L2)

    // Match the two image descriptors
    val matches = new DMatch()
    // "match" is a keyword in Scala, to avoid conflict between a keyword and a method match of the BFMatcher,
    // we need to enclose method name in ticks: `match`.
    matcher.`match`(descriptorsRight, descriptorsLeft, matches, null)

    println("Number of matched points: " + matches.capacity())

    // Select few matches, use similar approach as in [[opencv2_cookbook.chapter08.Ex7DescribingSURF]]
    val selectedMatches = selectBest(matches, matches.capacity / 2)

    // Use 7-Point method
    fundamentalMatrix7Point()

    // Use RANSAC method
    fundamentalMatrix7RANSAC()

    //----------------------------------------------------------------------------------------------------------------


    /** Compute fundamental matrix using 7-Point method. */
    def fundamentalMatrix7Point() {
        // Select 7 points that match well and are spread around the image. This is done by manually inspecting the points.
        val selected7Matches = new DMatch(7)
        copy(selectedMatches.position(0), selected7Matches.position(0))
        copy(selectedMatches.position(1), selected7Matches.position(1))
        copy(selectedMatches.position(2), selected7Matches.position(2))
        copy(selectedMatches.position(5), selected7Matches.position(3))
        copy(selectedMatches.position(6), selected7Matches.position(4))
        copy(selectedMatches.position(7), selected7Matches.position(5))
        copy(selectedMatches.position(8), selected7Matches.position(6))
        selected7Matches.position(0)

        // Draw selected matches
        val imageMatches = IplImage.create(
            new CvSize(imageRight.width + imageLeft.width, imageRight.height), imageRight.depth, 3)
        drawMatches(
            imageRight, keypointsRight, imageLeft, keypointsLeft, selected7Matches, imageMatches,
            CvScalar.BLUE, CvScalar.RED, null, DrawMatchesFlags.DEFAULT)
        show(imageMatches, "Matches 7-point")


        // Extract keypoints from each match, separate Left and Right
        val pointIndexesRight = new Array[Int](selected7Matches.capacity)
        val pointIndexesLeft = new Array[Int](selected7Matches.capacity)
        for (i <- 0 until selected7Matches.capacity) {
            pointIndexesRight(i) = selected7Matches.position(i).queryIdx()
            pointIndexesLeft(i) = selected7Matches.position(i).trainIdx()
        }

        // Convert keypoints into Point2f
        val selPointsRight = new CvPoint2D32f(selected7Matches.capacity)
        val selPointsLeft = new CvPoint2D32f(selected7Matches.capacity)
        KeyPoint.convert(keypointsRight, selPointsRight, pointIndexesRight)
        KeyPoint.convert(keypointsLeft, selPointsLeft, pointIndexesLeft)

        // Check by drawing the points
        show(drawOnImage(imageRight, selPointsRight), "selPointsRight")
        show(drawOnImage(imageLeft, selPointsLeft), "selPointsLeft")

        // Convert to CvMat as needed by `cvFindFundamentalMat`
        val selPointsMatRight = toCvMat(selPointsRight)
        val selPointsMatLeft = toCvMat(selPointsLeft)

        // Compute F matrix from 7 matches
        // The 7-point method of computing the fundamental matrix may return up to 3 solutions, so use 9x3 matrix
        val fundamentalMatrix = CvMat.create(9, 3, CV_32F)
        val numberOfSolutions = cvFindFundamentalMat(
            selPointsMatRight /* points in first image */ ,
            selPointsMatLeft /* points in second image */ ,
            fundamentalMatrix /* output */ ,
            CV_FM_7POINT /* 7-point method */ ,
            3, 0.99, null /* additional parameters with default values, not used by the 7-point method */)
        println("Number of 7-point solutions: " + numberOfSolutions)

        // Select the first solution
        val fundamentalMatrix1 = CvMat.create(3, 3)
        cvGetSubRect(fundamentalMatrix, fundamentalMatrix1, new CvRect(0, 0, 3, 3))

        // Draw the left points corresponding epipolar lines in right image
        val lines1 = CvMat.create(selPointsMatRight.rows, 3, CV_32F, 1)
        cvComputeCorrespondEpilines(
            selPointsMatRight,
            1 /* in image 1 (can also be 2) */ ,
            fundamentalMatrix1 /* F matrix */ ,
            lines1 /* epipolar lines */)
        show(drawEpiLines(imageLeft, lines1, selPointsLeft, null), "Left Image Epilines")

        // Draw the right points corresponding epipolar lines in left image
        val lines2 = CvMat.create(selPointsMatLeft.rows, 3, CV_32F, 1)
        cvComputeCorrespondEpilines(selPointsMatLeft, 2, fundamentalMatrix1, lines2)
        show(drawEpiLines(imageRight, lines2, selPointsRight, null), "Right Image Epilines")
    }


    /** Compute fundamental matrix using RANSAC method. */
    def fundamentalMatrix7RANSAC() {

        // Convert keypoints into Point2f
        val (selPointsRight, selPointsLeft) = MatcherUtils.toCvPoint2D32f(selectedMatches, keypointsRight, keypointsLeft)

        // Check by drawing the points
        show(drawOnImage(imageRight, selPointsRight), "selPointsRight (RANSAC)")
        show(drawOnImage(imageLeft, selPointsLeft), "selPointsLeft (RANSAC)")

        // Convert to CvMat as needed by `cvFindFundamentalMat`
        val selPointsMatRight = toCvMat(selPointsRight)
        val selPointsMatLeft = toCvMat(selPointsLeft)

        // Compute F matrix
        val fundamentalMatrix = CvMat.create(3, 3, CV_32F)
        val pointStatus = CvMat.create(selPointsRight.capacity, 1, CV_8U)
        cvFindFundamentalMat(
            selPointsMatRight /* points in first image */ ,
            selPointsMatLeft /* points in second image */ ,
            fundamentalMatrix /* output */ ,
            CV_FM_RANSAC /* RANSAC method */ ,
            1, /* distance to epipolar plane */
            0.98 /* confidence probability */ ,
            pointStatus)

        // Draw the left points corresponding epipolar lines in right image
        val lines1 = CvMat.create(selPointsMatRight.rows, 3, CV_32F, 1)
        cvComputeCorrespondEpilines(
            selPointsMatRight,
            1 /* in image 1 (can also be 2) */ ,
            fundamentalMatrix /* F matrix */ ,
            lines1 /* epipolar lines */)
        show(drawEpiLines(imageLeft, lines1, selPointsLeft, pointStatus), "Left Image Epilines (RANSAC)")

        // Draw the right points corresponding epipolar lines in left image
        val lines2 = CvMat.create(selPointsMatLeft.rows, 3, CV_32F, 1)
        cvComputeCorrespondEpilines(selPointsMatLeft, 2, fundamentalMatrix, lines2)
        show(drawEpiLines(imageRight, lines2, selPointsRight, pointStatus), "Right Image Epilines (RANSAC)")

    }


    private def drawKeyPoints(image: IplImage, keypoints: KeyPoint, title: String) {
        val canvas = IplImage.create(cvGetSize(image), image.depth(), 3)
        drawKeypoints(image, keypoints, canvas, CvScalar.WHITE, DrawMatchesFlags.DRAW_RICH_KEYPOINTS)
        show(canvas, title)
    }


    private def drawEpiLines(image: IplImage, lines: CvMat, points: CvPoint2D32f, inliers: CvMat): IplImage = {
        val canvas = IplImage.create(cvGetSize(image), image.depth(), 3)
        cvCvtColor(image, canvas, CV_GRAY2BGR)
        for (i <- 0 until lines.rows()) {
            val inlier = inliers != null && math.round(inliers.get(i)) != 0
            if (inlier) {
                // draw the epipolar line between first and last column
                val a = lines.get(i, 0, 0)
                val b = lines.get(i, 0, 1)
                val c = lines.get(i, 0, 2)
                val x0 = 0
                val y0 = math.round(-(c + a * x0) / b).toInt
                val x1 = image.width
                val y1 = math.round(-(c + a * x1) / b).toInt
                cvLine(canvas, new CvPoint(x0, y0), new CvPoint(x1, y1), CvScalar.WHITE, 1, CV_AA, 0)
            }
            val xp = math.round(points.position(i).x())
            val yp = math.round(points.position(i).y())
            val (color, width) = if (inlier) (CvScalar.RED, 2) else (CvScalar.YELLOW, 1)
            cvCircle(canvas, new CvPoint(xp, yp), 3, color, width, CV_AA, 0)
        }
        points.position(0)
        canvas
    }


    /** Select only the best matches from the list. Return new list. */
    private def selectBest(matches: DMatch, numberToSelect: Int): DMatch = {
        // Convert to Scala collection, and sort
        val sorted = toArray(matches).sortWith(_.compare(_))

        // Select the best, and return in native vector
        toNativeVector(sorted.take(numberToSelect))
    }
}
