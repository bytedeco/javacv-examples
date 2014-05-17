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
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_nonfree.SURF


/** The example for section "Computing a homography between two images" in Chapter 9, page 242.
  *
  * Most of the computations are done by `RobustMatcher` helper class.
  */
object Ex5Homography extends App {

  // Read input images
  val image1 = loadMatOrExit(new File("data/parliament1.bmp"))
  val image2 = loadMatOrExit(new File("data/parliament2.bmp"))
  show(image1, "Image 1")
  show(image2, "Image 2")

  // Prepare the matcher
  val rMatcher = new RobustMatcher(
    confidenceLevel = 0.98,
    minDistanceToEpipolar = 1.0,
    ratio = 0.85F,
    detector = new SURF(10, 4, 2, true, false),
    refineF = true
  )

  // Match the two images
  val matches = rMatcher.matchImages(image1, image2)

  // Draw the matches
  val matchesCanvas = new Mat(image1.cols() + image2.cols(), image1.rows(), CV_8UC3)
  val white = new Scalar(255, 255, 255, 0)
  val matchMask: Array[Byte] = null
  drawMatches(image1, matches.keyPoints1, image2, matches.keyPoints2,
    toNativeVector(matches.matches), matchesCanvas, white, Scalar.all(-1), matchMask, DrawMatchesFlags.DEFAULT)
  show(matchesCanvas, "Matches")

  // Convert keypoints into Point2f
  val (points1, points2) = MatcherUtils.toCvPoint2D32fPair(matches.matches, matches.keyPoints1, matches.keyPoints2)
  println("" + points1.capacity() + " " + points2.capacity())

  // Find the homography between image 1 and image 2
  val h = cvCreateMat(3, 3, CV_32F)
  val ok = cvFindHomography(toCvMat(points1), toCvMat(points2), h, CV_RANSAC, 1, null)
  if (ok == 0) {
    throw new Exception("Computation of homography failed.")
  }

  // Warp image 1 to image 2
  val im1Ipl = image1.asIplImage()
  val result = cvCreateImage(cvSize(2 * im1Ipl.width, im1Ipl.height), im1Ipl.depth, im1Ipl.nChannels)
  cvWarpPerspective(im1Ipl, result, h)

  // Copy image 2 on the first half of full image
  val roi = new IplROI()
  roi.width(im1Ipl.width)
  roi.height(im1Ipl.height)
  cvCopy(image2.asIplImage(), result.roi(roi))

  // Display the warp image
  show(result, "After warping")
}
