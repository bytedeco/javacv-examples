/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter10

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer.DoubleRawIndexer
import org.bytedeco.opencv.opencv_core.Size


/**
 * The main example for section "Calibrating a camera" in Chapter 9, page 221.
 */
object Ex2CalibrateCamera extends App {

  // Generate file list
  val fileList = for (i <- 1 to 20) yield new File("data/chessboards/chessboard%02d.jpg".format(i))

  // Create calibrator object
  val cameraCalibrator = new CameraCalibrator()

  // Add the corners from the chessboard
  println("Adding chessboard points from images...")
  val boardSize = new Size(6, 4)
  cameraCalibrator.addChessboardPoints(fileList, boardSize)

  // Load image for that will be undistorted
  val image = loadAndShowOrExit(fileList(6))

  // Calibrate camera
  println("Calibrating...")
  cameraCalibrator.calibrate(image.size())

  // Undistort
  println("Undistorting...")
  val undistorted = cameraCalibrator.remap(image)

  // Display camera matrix
  val m     = cameraCalibrator.cameraMatrix
  val mIndx = m.createIndexer().asInstanceOf[DoubleRawIndexer]
  println("Camera intrinsic: " + m.rows + "x" + m.cols)
  for (i <- 0 until 3) {
    for (j <- 0 until 3) {
      print("%7.2f  ".format(mIndx.get(i, j)))
    }
    println("")
  }

  show(undistorted, "Undistorted image.")
}
