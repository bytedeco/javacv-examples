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
import com.googlecode.javacv.cpp.opencv_nonfree.SURF
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/** The example for section "Matching images using random sample consensus" in Chapter 9, page 233.
  *
  * Most of the computations are done by `RobustMatcher` helper class.
  */
object Ex4MatchingUsingSampleConsensus extends App {

    // Read input images
    val image1 = loadOrExit(new File("data/canal1.jpg"))
    val image2 = loadOrExit(new File("data/canal2.jpg"))
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
    val matchesCanvas = IplImage.create(new CvSize(image1.width + image2.width, image1.height), image1.depth, 3)
    drawMatches(image1, matches.keyPoints1, image2, matches.keyPoints2,
        toNativeVector(matches.matches), matchesCanvas, CvScalar.WHITE, cvScalarAll(-1), null, DrawMatchesFlags.DEFAULT)
    show(matchesCanvas, "Matches")

    // Draw the epipolar lines
    val (points1, points2) = MatcherUtils.toCvPoint2D32f(matches.matches, matches.keyPoints1, matches.keyPoints2)
    val lines1 = CvMat.create(points1.capacity, 3, CV_32F, 1)
    cvComputeCorrespondEpilines(toCvMat(points1), 1, matches.fundamentalMatrix, lines1)
    show(drawEpiLines(image2, lines1, points2), "Left Image Epilines (RANSAC)")
    val lines2 = CvMat.create(points2.capacity, 3, CV_32F, 1)
    cvComputeCorrespondEpilines(toCvMat(points2), 2, matches.fundamentalMatrix, lines2)
    show(drawEpiLines(image1, lines2, points1), "Right Image Epilines (RANSAC)")


    //----------------------------------------------------------------------------------------------------------------


    /** Draw `epilines` and `points` on a color copy of an `image`.
      *
      * @return new image  with epilines and points.
      */
    private def drawEpiLines(image: IplImage, epilines: CvMat, points: CvPoint2D32f): IplImage = {
        val canvas = IplImage.create(cvGetSize(image), image.depth(), 3)
        cvCvtColor(image, canvas, CV_GRAY2BGR)
        for (i <- 0 until epilines.rows()) {
            // draw the epipolar line between first and last column
            val a = epilines.get(i, 0, 0)
            val b = epilines.get(i, 0, 1)
            val c = epilines.get(i, 0, 2)
            val x0 = 0
            val y0 = math.round(-(c + a * x0) / b).toInt
            val x1 = image.width
            val y1 = math.round(-(c + a * x1) / b).toInt
            cvLine(canvas, new CvPoint(x0, y0), new CvPoint(x1, y1), CvScalar.WHITE, 1, CV_AA, 0)

            val xp = math.round(points.position(i).x())
            val yp = math.round(points.position(i).y())
            cvCircle(canvas, new CvPoint(xp, yp), 3, CvScalar.RED, 1, CV_AA, 0)
        }
        points.position(0)
        canvas
    }
}
