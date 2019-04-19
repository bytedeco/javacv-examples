/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer.UByteRawIndexer
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_video._
import org.bytedeco.opencv.opencv_core._

import scala.collection.mutable.ArrayBuffer
import scala.math.abs


/** Detect and track moving features in a series of images.
  *
  * Described in section "Tracking feature points in video", chapter 10.
  *
  * @param maxCount 	maximum number of features to detect
  * @param qLevel   quality level for feature detection
  * @param minDist minimum distance between two feature points
  */
class FeatureTracker(maxCount: Int = 500,
                     qLevel: Double = 0.01,
                     minDist: Double = 10) {

  val minNumberOfTrackedPoints = 10

  /** initial position of tracked points */
  private var initialPositions = new ArrayBuffer[Point2f]()

  /** tracked features from 0->1 */
  private var trackedPoints = new ArrayBuffer[Point2f]()

  /** previous gray-level image */
  private var grayPrevious: Mat = _

  private var grayCurrent = new Mat()


  /** Process next frame.  */
  def process(inputFrame: Mat, outputFrame: Mat): Unit = {

    // convert to gray-level image
    cvtColor(inputFrame, grayCurrent, COLOR_BGR2GRAY)
    inputFrame.copyTo(outputFrame)

    // 1. Check if additional new feature points should be added
    if (shouldAddNewPoints) {
      // detect feature points
      val features = detectFeaturePoints(grayCurrent)
      // add the detected features to the currently tracked features
      initialPositions ++= features
      trackedPoints ++= features
    }

    // for first image of the sequence
    if (grayPrevious == null) {
      grayPrevious = new Mat()
      grayCurrent.copyTo(grayPrevious)
    }

    // 2. track features
    val trackingStatus = new Mat()
    val trackedPointsNewUnfilteredMat = new Mat()
    val err = new Mat()
    calcOpticalFlowPyrLK(
      grayPrevious, grayCurrent, // 2 consecutive images
      toMatPoint2f(trackedPoints), // input point position in first image
      trackedPointsNewUnfilteredMat, // output point position in the second image
      trackingStatus, // tracking success
      err // tracking error
    )

    // 3. loop over the tracked points to reject the undesirables
    val trackedPointsNewUnfiltered = toPoint2fArray(trackedPointsNewUnfilteredMat)
    val initialPositionsNew = new ArrayBuffer[Point2f]()
    val trackedPointsNew = new ArrayBuffer[Point2f]()
    val trackingStatusIndexer = trackingStatus.createIndexer().asInstanceOf[UByteRawIndexer]
    for (i <- trackedPointsNewUnfiltered.indices) {
      if (acceptTrackedPoint(trackingStatusIndexer.get(i), trackedPoints(i), trackedPointsNewUnfiltered(i))) {
        initialPositionsNew += initialPositions(i)
        trackedPointsNew += trackedPointsNewUnfiltered(i)
      }
    }

    // 4. handle the accepted tracked points
    handleTrackedPoints(initialPositionsNew, trackedPointsNew, inputFrame, outputFrame)

    // 5. current points and image become previous ones
    trackedPoints = trackedPointsNew
    initialPositions = initialPositionsNew
    // swap
    val h = grayPrevious
    grayPrevious = grayCurrent
    grayCurrent = h

  }


  /** Feature point detection. */
  def detectFeaturePoints(grayFrame: Mat): Array[Point2f] = {

    val features = new Mat()
    goodFeaturesToTrack(grayFrame, // the image
      features, // the output detected features
      maxCount, // the maximum number of features
      qLevel, // quality level
      minDist // min distance between two features
    )

    toPoint2fArray(features)
  }


  /** Determine if new points should be added. */
  private def shouldAddNewPoints: Boolean = trackedPoints.size < minNumberOfTrackedPoints


  /** Determine if a tracked point should be accepted. */
  def acceptTrackedPoint(status: Int, point0: Point2f, point1: Point2f): Boolean = {
    status != 0 &&
      // if point has moved
      (abs(point0.x - point1.x) + abs(point0.y - point1.y) > 2)
  }


  /** display the currently tracked points */
  def handleTrackedPoints(startPoints: Seq[Point2f],
                          endPoints: Seq[Point2f],
                          frame: Mat,
                          output: Mat) {

    // for all tracked points
    for (i <- startPoints.indices) {
      val startPoint = toPoint(startPoints(i))
      val endPoint = toPoint(endPoints(i))
      // Mark tracked point movement with aline
      line(output, startPoint, endPoint, new Scalar(255, 255, 255, 0))
      // Mark starting point with circle
      circle(output, startPoint, 3, new Scalar(255, 255, 255, 0), -1, LINE_AA, 0)
    }
  }

}
