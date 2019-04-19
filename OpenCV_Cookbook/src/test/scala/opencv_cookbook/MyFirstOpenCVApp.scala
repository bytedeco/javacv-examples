/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook

import javax.swing._
import org.bytedeco.javacv._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

object MyFirstOpenCVApp extends App {

  // Read an image.
  val src = imread("data/boldt.jpg")
  display(src, "Input")

  // Apply Laplacian filter
  val dest = new Mat()
  Laplacian(src, dest, src.depth(), 1, 3, 0, BORDER_DEFAULT)
  display(dest, "Laplacian")

  //---------------------------------------------------------------------------

  /** Display `image` with given `caption`. */
  def display(image: Mat, caption: String): Unit = {
    // Create image window named "My Image."
    val canvas = new CanvasFrame(caption, 1)

    // Request closing of the application when the image window is closed.
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    // Convert from OpenCV Mat to Java Buffered image for display
    val converter = new OpenCVFrameConverter.ToMat()
    // Show image on window
    canvas.showImage(converter.convert(image))
  }
}
