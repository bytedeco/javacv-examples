/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter09

import java.io.{IOException, File}
import javax.swing.WindowConstants
import org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar
import org.bytedeco.javacpp.opencv_calib3d._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv.CanvasFrame
import scala.collection.mutable.ArrayBuffer


/** Camera calibration helper, used in example `Ex2CalibrateCamera`. */
class CameraCalibrator {

  private case class Point3D(x: Float, y: Float, z: Float)


  // Input points
  private val objectPoints = new ArrayBuffer[Seq[Point3D]]
  private val imagePoints = new ArrayBuffer[Array[Float]]

  // Output matrices
  private val _cameraMatrix = cvCreateMat(3, 3, CV_32F)
  private val _distortionCoeffs = cvCreateMat(1, 8, CV_32F)

  // flag to specify how calibration is done
  private var flag: Int = 0

  private var mustInitUndistort = true


  /** Return a copy of the camera matrix. */
  def cameraMatrix = _cameraMatrix.clone()


  /** Return a copy of the distortion coefficients. */
  def distortionCoeffs = _distortionCoeffs.clone()


  /** Set the calibration options.
    *
    * @param radial8CoeffEnabled should be true if 8 radial coefficients are required (5 is default).
    * @param tangentialParamEnabled should be true if tangential distortion is present.
    */
  def setCalibrationFlag(radial8CoeffEnabled: Boolean, tangentialParamEnabled: Boolean) {
    // Set the flag used in cv::calibrateCamera()
    flag = 0
    if (!tangentialParamEnabled) flag += CV_CALIB_ZERO_TANGENT_DIST
    if (radial8CoeffEnabled) flag += CV_CALIB_RATIONAL_MODEL
  }

  /** Open chessboard images and extract corner points. */
  def addChessboardPoints(fileList: Seq[File], boardSize: CvSize): Int = {
    objectPoints.clear()
    imagePoints.clear()

    // 3D Scene Points:
    // Initialize the chessboard corners
    // in the chessboard reference frame
    // The corners are at 3D location (X,Y,Z)= (i,j,0)
    val objectCorners = for (i <- 0 until boardSize.height; j <- 0 until boardSize.width) yield Point3D(i, j, 0)

    // 2D Image points:
    var successes = 0
    // for all viewpoints
    for (file <- fileList) {

      // Open the image
      val image = cvLoadImage(file.getAbsolutePath, CV_LOAD_IMAGE_GRAYSCALE)
      if (image == null) {
        throw new IOException("Couldn't load image: " + file.getAbsolutePath)
      }

      // Get the chessboard corners
      // Allocate array to pass back corner coordinates: (x0, y0, x1, y1, ...)
      val imageCorners = new Array[Float](boardSize.width * boardSize.height * 2)
      val cornerCount = Array(1)
      val found = cvFindChessboardCorners(image, boardSize, imageCorners, cornerCount,
        CV_CALIB_CB_ADAPTIVE_THRESH | CV_CALIB_CB_NORMALIZE_IMAGE)

      // Get subpixel accuracy on the corners
      cvFindCornerSubPix(
        image, imageCorners, cornerCount(0), cvSize(5, 5), cvSize(-1, -1),
        cvTermCriteria(
          CV_TERMCRIT_ITER + CV_TERMCRIT_EPS,
          30, // max number of iterations
          0.1) // min accuracy
      )

      // If we have a good board, add it to our data
      if (cornerCount(0) == boardSize.width * boardSize.height) {
        // Add image and scene points from one view
        imagePoints += imageCorners
        objectPoints += objectCorners
        successes += 1
      }

      // Draw the corners
      cvDrawChessboardCorners(image, boardSize, imageCorners, cornerCount(0), found)
      val canvas = new CanvasFrame("Corners on Chessboard: " + file.getName, 1)
      canvas.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      canvas.showImage(image)
    }

    successes
  }


  /** Calibrate the camera.
    *
    * @return final re-projection error.
    */
  def calibrate(imageSize: CvSize): Double = {
    // undistorter must be reinitialized
    mustInitUndistort = true

    // Prepare object and image points in format suitable for `cvCalibrateCamera2`
    val (objectPointsCvMat, imagePointsCvMat, pointsCountsCvMat) = convertPoints()

    //Output rotations and translations
    val rotationVectors = new CvMat(null)
    val translationVectors = new CvMat(null)
    cvCalibrateCamera2(
      objectPointsCvMat, // the 3D points
      imagePointsCvMat, // the image points
      pointsCountsCvMat,
      imageSize, // image size
      _cameraMatrix, // output camera matrix
      _distortionCoeffs, // output distortion matrix
      rotationVectors, translationVectors, // Rs, Ts
      flag, // set options
      cvTermCriteria(CV_TERMCRIT_ITER + CV_TERMCRIT_EPS, 30, Double.MinPositiveValue)
    )
  }


  /** Remove distortion in an image (after calibration). */
  def remap(image: IplImage): IplImage = {

    val xMap = cvCreateMat(image.height, image.width, CV_32F)
    val yMap = cvCreateMat(image.height, image.width, CV_32F)
    if (mustInitUndistort) {
      // Called once per calibration
      cvInitUndistortRectifyMap(
        _cameraMatrix,
        _distortionCoeffs,
        null,
        null,
        xMap, yMap // the x and y mapping functions
      )
      mustInitUndistort = false
    }

    // Apply mapping functions
    val remapped = cvCreateImage(cvGetSize(image), image.depth, 1)
    cvRemap(image, remapped, xMap, yMap, CV_INTER_LINEAR, AbstractCvScalar.ZERO)

    remapped
  }

  /** Prepare object points, image points, and point counts in format required by `cvCalibrateCamera2`. */
  private def convertPoints(): (CvMat, CvMat, CvMat) = {

    require(objectPoints.size == imagePoints.size, "Number of object and image points must match.")

    val pointCounts = cvCreateMat(1, objectPoints.size, CV_32SC1)
    val pointCountsBuf = pointCounts.getIntBuffer
    var totalPointCount = 0
    for ((objectP, imageP) <- objectPoints zip imagePoints) {
      require(objectP.length == imageP.size / 2, s"${objectP.length} != ${imageP.size / 2}")
      val n = objectP.length
      pointCountsBuf.put(n)
      totalPointCount += n
    }

    val objectPointsCvMat = cvCreateMat(1, totalPointCount, CV_32FC3)
    val imagePointsCvMat = cvCreateMat(1, totalPointCount, CV_32FC2)
    val objectPointsBuf = objectPointsCvMat.getFloatBuffer
    val imagePointsBuf = imagePointsCvMat.getFloatBuffer
    for ((objectP, imageP) <- objectPoints zip imagePoints) {

      for (j <- 0 until objectP.length) {
        objectPointsBuf.put(objectP(j).x)
        objectPointsBuf.put(objectP(j).y)
        objectPointsBuf.put(objectP(j).z)

        imagePointsBuf.put(imageP(2 * j))
        imagePointsBuf.put(imageP(2 * j + 1))
      }
    }

    (objectPointsCvMat, imagePointsCvMat, pointCounts)
  }
}
