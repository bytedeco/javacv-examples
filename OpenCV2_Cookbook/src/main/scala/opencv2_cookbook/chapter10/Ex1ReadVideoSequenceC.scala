/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter10

import javax.swing.JFrame
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacv.CanvasFrame


/** The example for section "Reading video sequences" in Chapter 10, page 248.
  *
  * This version of the example is implemented using OpenCV C API.
  *
  * @see opencv2_cookbook.chapter10.Ex1ReadVideoSequenceJavaCV
  */
object Ex1ReadVideoSequenceC extends App {

  // Open video video file
  val capture = cvCreateFileCapture("data/bike.avi")

  // Prepare window to display frames
  val canvasFrame = new CanvasFrame("Extracted Frame", 1)
  val width = cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH).toInt
  val height = cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_HEIGHT).toInt
  canvasFrame.setCanvasSize(width, height)
  // Exit the example when the canvas frame is closed
  canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

  // Time between frames in the video
  val delay = math.round(1000d / cvGetCaptureProperty(capture, CV_CAP_PROP_FPS))

  // Read frame by frame, stop early if the display window is closed
  var frame: IplImage = null
  while (cvGrabFrame(capture) != 0 && {frame = cvRetrieveFrame(capture); frame} != null) {
    // Show the frame
    canvasFrame.showImage(frame)
    // Delay
    Thread.sleep(delay)
  }

  // Close the video file
  cvReleaseCapture(capture)
}
