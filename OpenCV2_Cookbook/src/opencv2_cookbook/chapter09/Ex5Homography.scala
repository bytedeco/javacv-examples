/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter09

import com.googlecode.javacv.cpp.opencv_calib3d._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_nonfree.SURF
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/** The example for section "Computing a homography between two images" in Chapter 9, page 242.
  *
  * Most of the computations are done by `RobustMatcher` helper class.
  */
object Ex5Homography extends App {

    // Read input images
    val image1 = loadOrExit(new File("data/parliament1.bmp"))
    val image2 = loadOrExit(new File("data/parliament2.bmp"))
    show(image1, "Image 1")
    show(image2, "Image 2")

    // Prepare the matcher
    val rMatcher = new RobustMatcher(
        confidenceLevel = 0.98,
        minDistanceToEpipolar = 1.0,
        ratio = 0.85F,
        detector = new SURF(10),
        refineF = true
    )

    // Match the two images
    val matches = rMatcher.matchImages(image1, image2)

    // Draw the matches
    val matchesCanvas = cvCreateImage(new CvSize(image1.width + image2.width, image1.height), image1.depth, 3)
    drawMatches(image1, matches.keyPoints1, image2, matches.keyPoints2,
        toNativeVector(matches.matches), matchesCanvas, CvScalar.WHITE, cvScalarAll(-1), null, DrawMatchesFlags.DEFAULT)
    show(matchesCanvas, "Matches")

    // Convert keypoints into Point2f
    val (points1, points2) = MatcherUtils.toCvPoint2D32f(matches.matches, matches.keyPoints1, matches.keyPoints2)
    println("" + points1.capacity() + " " + points2.capacity())

    // Find the homography between image 1 and image 2
    val h = CvMat.create(3, 3)
    val ok = cvFindHomography(toCvMat(points1), toCvMat(points2), h, CV_RANSAC, 1, null)
    if (ok == 0) {
        throw new Exception("Computation of homography failed.")
    }

    // Warp image 1 to image 2
    val result = cvCreateImage(new CvSize(2 * image1.width, image1.height), image1.depth, image1.nChannels)
    cvWarpPerspective(image1, result, h)

    // Copy image 2 on the first half of full image
    val roi = new IplROI()
    roi.width(image2.width)
    roi.height(image2.height)
    cvCopy(image2, result.roi(roi))

    // Display the warp image
    show(result, "After warping")
}
