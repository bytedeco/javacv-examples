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
import org.bytedeco.javacpp.indexer.UByteRawIndexer
import org.bytedeco.opencv.global.opencv_calib3d._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_features2d._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._
import org.bytedeco.opencv.opencv_features2d._
import org.bytedeco.opencv.opencv_xfeatures2d._

import scala.math.round


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


  // 0. Construction of the detector and descriptor
  // for example SURF
  val detector  = SURF.create()
  val extractor = detector

  // 1. Detection of the feature points
  val keypoints1 = new KeyPointVector()
  val keypoints2 = new KeyPointVector()
  detector.detect(image1, keypoints1)
  detector.detect(image2, keypoints2)

  println("Number of feature points (1): " + keypoints1.size)
  println("Number of feature points (2): " + keypoints2.size)

  // 2. Extraction of the feature descriptors
  val descriptors1 = new Mat()
  val descriptors2 = new Mat()
  extractor.compute(image1, keypoints1, descriptors1)
  extractor.compute(image2, keypoints2, descriptors2)

  // 3. Match the two image descriptors

  // Construction of the matcher with crosscheck
  val matcher = new BFMatcher(NORM_L2, true)
  // matching
  val matches = new DMatchVector()
  matcher.`match`(descriptors1, descriptors2, matches)

  // draw the matches
  val imageMatches = new Mat()
  drawMatches(
    image1, keypoints1, // 1st image and its keypoints
    image2, keypoints2, // 2nd image and its keypoints
    matches, // the matches
    imageMatches, // the image produced
    new Scalar(255, 255, 255, 0), // color of the lines
    new Scalar(255, 255, 255, 0), // color of the keypoints
    null.asInstanceOf[ByteBuffer], // empty mask
    2)
  show(imageMatches, "Matches")

  // Convert keypoints into Point2f
  val (points1, points2) = toPoint2fVectorPair(matches, keypoints1, keypoints2)

  println(points1.size() + " " + points2.size())

  // Find the homography between image 1 and image 2
  val inliers    = new Mat()
  val homography = findHomography(
    toMat(points1), toMat(points2), // corresponding points
    inliers, // outputted inliers matches
    RANSAC, // RANSAC method
    1.0 // max distance to reprojection point
  )


  // Draw the inlier points
  val inlinerIndexer = inliers.createIndexer().asInstanceOf[UByteRawIndexer]
  for (i <- 0 until points1.size.toInt if inlinerIndexer.get(i) != 0) {
    val p = points1.get(i)
    circle(image1, new Point(round(p.x), round(p.y)), 3, new Scalar(255, 255, 255, 0))
  }
  for (i <- 0 until points2.size.toInt if inlinerIndexer.get(i) != 0) {
    val p = points2.get(i)
    circle(image2, new Point(round(p.x), round(p.y)), 3, new Scalar(255, 255, 255, 0))
  }
  show(image1, "Image 1 Homography Points")
  show(image2, "Image 2 Homography Points")

  // Warp image 1 to image 2
  val result = new Mat()
  warpPerspective(image1, // input image
    result, // output image
    homography, // homography
    new Size(2 * math.max(image1.cols, image2.cols), math.max(image1.rows, image2.rows())) // size of output image
  )


  // Copy image 1 on the first half of full image
  val half = new Mat(result, new Rect(0, 0, image2.cols, image2.rows))
  image2.copyTo(half)

  // Display the warp image
  show(result, "Image mosaic")
}
