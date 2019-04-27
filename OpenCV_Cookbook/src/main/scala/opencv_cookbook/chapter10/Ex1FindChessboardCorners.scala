/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter10

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_calib3d._
import org.bytedeco.opencv.opencv_core._

/**
 * The first example for section "Calibrating a camera" in Chapter 9, page 219.
 *
 * Illustrates one of calibration steps, detection of a chessboard pattern in a calibration board.
 */
object Ex1FindChessboardCorners extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/chessboards/chessboard07.jpg"))

  // Find chessboard corners
  val boardSize    = new Size(6, 4)
  // Allocate array to pass back corner coordinates: (x0, y0, x1, y1, ...)
  val imageCorners = new Mat()
  val patternFound = findChessboardCorners(image, boardSize, imageCorners)

  // Draw the corners
  drawChessboardCorners(image, boardSize, imageCorners, patternFound)
  show(image, "Corners on Chessboard")
}
