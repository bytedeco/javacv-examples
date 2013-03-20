/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter10

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_video._
import opencv2_cookbook.OpenCVUtils._
import scala.collection.mutable.ArrayBuffer


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
    private var initialPositions = new ArrayBuffer[CvPoint2D32f]()

    /** tracked features from 0->1 */
    private var trackedPoints = new ArrayBuffer[CvPoint2D32f]()

    /** previous gray-level image */
    private var grayPrevious: IplImage = null


    /** Process next frame.  */
    def process(frame: IplImage): IplImage = {

        // convert to gray-level image
        val grayCurrent = IplImage.create(cvGetSize(frame), frame.depth, 1)
        cvCvtColor(frame, grayCurrent, CV_BGR2GRAY)

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
            grayPrevious = IplImage.create(cvGetSize(grayCurrent), grayCurrent.depth, grayCurrent.nChannels)
            cvCopy(grayCurrent, grayPrevious, null)
        }

        // 2. track features
        val trackedPointsNewUnfilteredOCV = new CvPoint2D32f(trackedPoints.length)
        val trackingStatus = new Array[Byte](trackedPoints.length)
        cvCalcOpticalFlowPyrLK(
            grayPrevious, grayCurrent, // 2 consecutive images
            null, null, // Unused
            toNativeVector(trackedPoints.toArray), // input point position in previous image
            trackedPointsNewUnfilteredOCV, // output point position in the current image
            trackedPoints.length,
            new CvSize(21, 21), 3, // Defaults
            trackingStatus, // tracking success
            new Array[Float](trackedPoints.length), // Not used
            new CvTermCriteria(CV_TERMCRIT_ITER + CV_TERMCRIT_EPS, 30, 0.01), 0 // defaults
        )

        // 2. loop over the tracked points to reject the undesirables
        val trackedPointsNewUnfiltered = toArray(trackedPointsNewUnfilteredOCV)
        val initialPositionsNew = new ArrayBuffer[CvPoint2D32f]()
        val trackedPointsNew = new ArrayBuffer[CvPoint2D32f]()
        for (i <- 0 until trackedPointsNewUnfiltered.size) {
            if (acceptTrackedPoint(trackingStatus(i), trackedPoints(i), trackedPointsNewUnfiltered(i))) {
                initialPositionsNew += initialPositions(i)
                trackedPointsNew += trackedPointsNewUnfiltered(i)
            }
        }

        // Prepare output
        val output = IplImage.create(cvGetSize(frame), frame.depth, frame.nChannels)
        cvCopy(frame, output, null)

        // 3. handle the accepted tracked points
        visualizeTrackedPoints(initialPositionsNew, trackedPointsNew, frame, output)

        // 4. current points and image become previous ones
        trackedPoints = trackedPointsNew
        initialPositions = initialPositionsNew
        grayPrevious = grayCurrent

        output
    }


    /** Feature point detection. */
    def detectFeaturePoints(grayFrame: IplImage): Array[CvPoint2D32f] = {

        val featurePoints = new CvPoint2D32f(maxCount)
        val featureCount = Array(maxCount)
        // detect the features
        cvGoodFeaturesToTrack(grayFrame, // the image
            null, null, // ignored parameters
            featurePoints, // the output detected features
            featureCount,
            qLevel, // quality level
            minDist, // min distance between two features
            null, 3, 0, 0.04 // Default parameters
        )

        // Select only detected features, end of the vector do not have valid entries
        toArray(featurePoints).take(featureCount(0))
    }


    /** Determine if new points should be added. */
    private def shouldAddNewPoints: Boolean = trackedPoints.size < minNumberOfTrackedPoints


    /** Determine if a tracked point should be accepted. */
    def acceptTrackedPoint(status: Int, point0: CvPoint2D32f, point1: CvPoint2D32f): Boolean = {
        status != 0 &&
                // if point has moved
                (math.abs(point0.x - point1.x) + (math.abs(point0.y - point1.y)) > 2)
    }


    /** display the currently tracked points */
    def visualizeTrackedPoints(startPoints: Seq[CvPoint2D32f],
                               endPoints: Seq[CvPoint2D32f],
                               frame: IplImage,
                               output: IplImage) {

        // for all tracked points
        for (i <- 0 until startPoints.length) {
            val startPoint = cvPointFrom32f(startPoints(i))
            val endPoint = cvPointFrom32f(endPoints(i))
            // Mark tracked point movement with aline
            cvLine(output, startPoint, endPoint, CvScalar.WHITE, 1, CV_AA, 0)
            // Mark starting point with circle
            cvCircle(output, startPoint, 3, CvScalar.WHITE, -1, CV_AA, 0)
        }
    }
}
