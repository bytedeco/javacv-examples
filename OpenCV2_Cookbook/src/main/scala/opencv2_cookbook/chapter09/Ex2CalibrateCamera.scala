/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter09

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._


/**
 * The main example for section "Calibrating a camera" in Chapter 9, page 221.
 */
object Ex2CalibrateCamera extends App {

  // Generate file list
  val fileList = for (i <- (1 to 20).toSeq) yield new File("data/chessboards/chessboard%02d.jpg".format(i))

  // Create calibrator object
  val cameraCalibrator = new CameraCalibrator()

  // Add the corners from the chessboard
  val boardSize = cvSize(6, 4)
  cameraCalibrator.addChessboardPoints(fileList, boardSize)

  // Load image for that will be undistorted
  val image = loadIplAndShowOrExit(fileList(6))

  // Calibrate camera
  cameraCalibrator.calibrate(cvGetSize(image))

  // Undistort
  val undistorted = cameraCalibrator.remap(image)

  // Display camera matrix
  val m = cameraCalibrator.cameraMatrix
  println("Camera intrinsic: " + m.rows + "x" + m.cols)
  for (i <- 0 until 3) {
    for (j <- 0 until 3) {
      print("%7.2f  ".format(m.get(i, j)))
    }
    println("")
  }

  show(undistorted, "Undistorted image.")
}
