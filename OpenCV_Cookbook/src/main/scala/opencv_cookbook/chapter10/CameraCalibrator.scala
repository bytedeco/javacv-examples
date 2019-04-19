/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter10

import java.io.{File, IOException}

import javax.swing.WindowConstants
import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.opencv.global.opencv_calib3d._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps


/** Camera calibration helper, used in example `Ex2CalibrateCamera`. */
class CameraCalibrator {


  // Input points
  private val objectPoints = new ArrayBuffer[Seq[Point3f]]
  private val imagePoints  = new ArrayBuffer[Mat]

  // Output matrices
  private val _cameraMatrix     = new Mat()
  private val _distortionCoeffs = new Mat()

  // flag to specify how calibration is done
  private var flag: Int = 0

  private var mustInitUndistort = true


  /** Return a copy of the camera matrix. */
  def cameraMatrix: Mat = _cameraMatrix.clone()


  /** Return a copy of the distortion coefficients. */
  def distortionCoeffs: Mat = _distortionCoeffs.clone()


  /** Set the calibration options.
    *
    * @param radial8CoeffEnabled should be true if 8 radial coefficients are required (5 is default).
    * @param tangentialParamEnabled should be true if tangential distortion is present.
    */
  def setCalibrationFlag(radial8CoeffEnabled: Boolean, tangentialParamEnabled: Boolean) {
    // Set the flag used in cv::calibrateCamera()
    flag = 0
    if (!tangentialParamEnabled) flag += CALIB_ZERO_TANGENT_DIST
    if (radial8CoeffEnabled) flag += CALIB_RATIONAL_MODEL
  }

  /** Open chessboard images and extract corner points. */
  def addChessboardPoints(fileList: Seq[File], boardSize: Size): Int = {
    objectPoints.clear()
    imagePoints.clear()

    // 3D Scene Points:
    // Initialize the chessboard corners
    // in the chessboard reference frame
    // The corners are at 3D location (X,Y,Z)= (i,j,0)
    val objectCorners = for (i <- 0 until boardSize.height; j <- 0 until boardSize.width) yield new Point3f(i, j, 0)

    // 2D Image points:
    var successes = 0
    // for all viewpoints
    for (file <- fileList) {

      // Open the image
      val image = imread(file.getAbsolutePath, IMREAD_GRAYSCALE)
      if (image == null) {
        throw new IOException("Couldn't load image: " + file.getAbsolutePath)
      }

      // Get the chessboard corners
      // Allocate array to pass back corner coordinates: (x0, y0, x1, y1, ...)
      val imageCorners = new Mat()
      //      val cornerCount = new Size()
      val found = findChessboardCorners(image, boardSize, imageCorners)

      // Get subpixel accuracy on the corners
      cornerSubPix(
        image, imageCorners, new Size(5, 5), new Size(-1, -1),
        new TermCriteria(
          TermCriteria.MAX_ITER + TermCriteria.EPS,
          30, // max number of iterations
          0.1) // min accuracy
      )

      // If we have a good board, add it to our data
      if (imageCorners.size().area() == boardSize.area()) {
        // Add image and scene points from one view
        imagePoints += imageCorners
        objectPoints += objectCorners
        successes += 1
      }

      // Draw the corners
      drawChessboardCorners(image, boardSize, imageCorners, found)
      val canvas = new CanvasFrame("Corners on Chessboard: " + file.getName, 1)
      canvas.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      canvas.showImage(toBufferedImage(image))
    }

    successes
  }


  /** Calibrate the camera.
    *
    * @return final re-projection error.
    */
  def calibrate(imageSize: Size): Double = {
    // undistorter must be reinitialized
    mustInitUndistort = true

    // Prepare object and image points in format suitable for `cvCalibrateCamera2`
    val (objectPointsMatVect, imagePointsMatVect) = convertPoints()

    //Output rotations and translations
    val rotationVectors = new MatVector()
    val translationVectors = new MatVector()
    calibrateCamera(
      objectPointsMatVect, // the 3D points
      imagePointsMatVect, // the image points
      imageSize, // image size
      _cameraMatrix, // output camera matrix
      _distortionCoeffs, // output distortion matrix
      rotationVectors, translationVectors, // Rs, Ts
      flag, // set options
      new TermCriteria(TermCriteria.MAX_ITER + TermCriteria.EPS, 30, Double.MinPositiveValue)
    )
  }


  /** Remove distortion in an image (after calibration). */
  def remap(image: Mat): Mat = {

    val undistorted = new Mat()
    val map1 = new Mat()
    val map2 = new Mat()
    if (mustInitUndistort) {
      // Called once per calibration
      initUndistortRectifyMap(
        _cameraMatrix, // computed camera matrix
        _distortionCoeffs, // computed distortion matrix
        new Mat(), // optional rectification (none)
        new Mat(), // camera matrix to generate undistorted
        image.size(), // size of undistorted
        CV_32FC1, // type of output map
        map1, map2); // the x and y mapping functions
      mustInitUndistort = false
    }

    // Apply mapping functions
    opencv_imgproc.remap(image, undistorted, map1, map2, INTER_LINEAR)

    undistorted
  }

  /** Prepare object points, image points, and point counts in format required by `cvCalibrateCamera2`. */
  private def convertPoints(): (Point3fVectorVector, Point2fVectorVector) = {

    require(objectPoints.size == imagePoints.size, "Number of object and image points must match.")

    val objectPointsVV = new Point3fVectorVector(objectPoints.size)
    val imagePointsVV = new Point2fVectorVector(objectPoints.size)
    for (((objectP, imageP), i) <- objectPoints zip imagePoints zipWithIndex) {
      objectPointsVV.put(i, new Point3fVector(objectP: _ *))
      imagePointsVV.put(i, new Point2fVector(toPoint2fArray(imageP): _ *))
    }

    (objectPointsVV, imagePointsVV)
  }
}
