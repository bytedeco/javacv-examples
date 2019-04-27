/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11

import javax.swing.WindowConstants
import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameGrabber}

import scala.collection.Iterator.continually


/** The example for section "Reading video sequences" in Chapter 10, page 248.
  *
  * This version of the example is implemented using JavaCV `FFmpegFrameGrabber`class.
  *
  */
object Ex1ReadVideoSequence extends App {

  val grabber = new FFmpegFrameGrabber("data/bike.avi")
  // Open video video file
  grabber.start()

  // Prepare window to display frames
  val canvasFrame = new CanvasFrame("Extracted Frame", 1)
  canvasFrame.setCanvasSize(grabber.getImageWidth, grabber.getImageHeight)
  // Exit the example when the canvas frame is closed
  canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  val delay = math.round(1000d / grabber.getFrameRate)

  // Read frame by frame, stop early if the display window is closed
  for (frame <- continually(grabber.grab()).takeWhile(_ != null)
       if canvasFrame.isVisible) {
    // Capture and show the frame
    canvasFrame.showImage(frame)
    // Delay
    Thread.sleep(delay)
  }

  // Close the video file
  grabber.release()
}
