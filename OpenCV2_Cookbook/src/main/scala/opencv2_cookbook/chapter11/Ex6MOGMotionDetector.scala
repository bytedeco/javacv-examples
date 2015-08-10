/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter11


import javax.swing.JFrame

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_video._
import org.bytedeco.javacpp.opencv_videoio._
import org.bytedeco.javacv.CanvasFrame


/** The second example example for section "Extracting the foreground objects in video" in Chapter 10, page 272.
  *
  * This version of foreground segmentation is using a more elaborate approach based on modeling background as a
  * mixture of Gaussians. It is using OpenCV class `BackgroundSubtractorMOG`.
  *
  * This version of the example is implemented using OpenCV C API.
  *
  * @see opencv2_cookbook.chapter11.Ex1ReadVideoSequenceJavaCV
  */
object Ex6MOGMotionDetector extends App {

  // Open video video file
  val capture = new VideoCapture("data/bike.avi")
  require(capture.isOpened, "Failed to open input video")

  // Prepare window to display frames
  val canvasFrame = new CanvasFrame("Extracted Foreground")
  val width       = capture.get(CAP_PROP_FRAME_WIDTH).toInt
  val height      = capture.get(CAP_PROP_FRAME_HEIGHT).toInt
  canvasFrame.setCanvasSize(width, height)
  canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

  // Time between frames in the video
  val delay = math.round(1000d / capture.get(CAP_PROP_FPS))

  // current video frame
  var frame      = new Mat()
  // foreground binary image
  var foreground = new Mat()
  // background image
  var background = new Mat()

  // Mixture of Gaussians approach
  //  val mog = new BackgroundSubtractorMOG2()
  val mog = createBackgroundSubtractorMOG2()

  var stop = false
  while (capture.read(frame)) {
    // update the background
    // and return the foreground
    mog(frame, foreground, 0.01)

    // Complement the image
    threshold(foreground, foreground, 128, 255, THRESH_BINARY_INV)

    canvasFrame.showImage(toBufferedImage(foreground))

    // Delay
    Thread.sleep(delay)
  }

  capture.release()
}
