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


/**
 * The first example for section "Calibrating a camera" in Chapter 9, page 219.
 *
 * Illustrates one of calibration steps, detection of a chessboard pattern in a calibration board.
 */
object Ex1FindChessboardCorners extends App {

  // Read input image
  val image = loadIplAndShowOrExit(new File("data/chessboards/chessboard07.jpg"))

  // Find chessboard corners
  val patternSize = cvSize(6, 4)
  // Allocate array to pass back corner coordinates: (x0, y0, x1, y1, ...)
  val corners = new Array[Float](patternSize.width * patternSize.height * 2)
  val cornerCount = Array(1)
  val flags = CV_CALIB_CB_ADAPTIVE_THRESH | CV_CALIB_CB_NORMALIZE_IMAGE
  val patternFound = cvFindChessboardCorners(image, patternSize, corners, cornerCount, flags)

  // Draw the corners
  cvDrawChessboardCorners(image, patternSize, corners, cornerCount(0), patternFound)
  show(image, "Corners on Chessboard")
}
