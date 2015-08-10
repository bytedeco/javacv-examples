/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter11

import javax.swing.JFrame

import org.bytedeco.javacpp.opencv_core.Mat
import org.bytedeco.javacpp.opencv_videoio._
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}

import scala.math.round

/** The example for section "Reading video sequences" in Chapter 10, page 248.
  *
  * This version of the example is implemented using OpenCV C API.
  *
  * @see opencv2_cookbook.chapter11.Ex1ReadVideoSequenceJavaCV
  */
object Ex1ReadVideoSequence extends App {

  // Open video file
  val capture = new VideoCapture("data/bike.avi")

  // check if video successfully opened
  require(capture.isOpened, "Failed to open input video")

  // Get the frame rate
  val rate = capture.get(CAP_PROP_FPS)
  println("Frame rate: " + rate + "fps")

  var stop        = false
  // current video frame
  val frame       = new Mat()
  val canvasFrame = new CanvasFrame("Extracted Frame", 1)
  val width       = capture.get(CAP_PROP_FRAME_WIDTH).toInt
  val height      = capture.get(CAP_PROP_FRAME_HEIGHT).toInt
  canvasFrame.setCanvasSize(width, height)
  // Exit the example when the canvas frame is closed
  canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

  //
  val canvasConverter = new OpenCVFrameConverter.ToMat()


  // Delay between each frame
  // corresponds to video frame rate
  val delay = round(1000 / rate)
  // for all frames in video
  while (!stop) {
    // read next frame if any
    if (capture.read(frame)) {
      canvasFrame.showImage(canvasConverter.convert(frame))
      // Introduce a delay
      Thread.sleep(delay)
    } else {
      stop = true
    }
  }

  // Close the video file
  capture.release()
}
