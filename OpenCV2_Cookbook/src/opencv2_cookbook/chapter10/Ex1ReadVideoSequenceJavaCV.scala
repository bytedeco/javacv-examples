/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter10

import com.googlecode.javacv.{CanvasFrame, OpenCVFrameGrabber}
import javax.swing.JFrame


/** The example for section "Reading video sequences" in Chapter 10, page 248.
  *
  * This version of the example is implemented using JavaCV `OpenCVFrameGrabber`class,
  * that is a wrapper for wraps OpenCV C API.
  *
  * @see opencv2_cookbook.chapter10.Ex1ReadVideoSequenceC
  */
object Ex1ReadVideoSequenceJavaCV extends App {

    val capture = new OpenCVFrameGrabber("data/bike.avi")
    // Open video video file
    capture.start()

    // Prepare window to display frames
    val canvasFrame = new CanvasFrame("Extracted Frame", 1)
    canvasFrame.setCanvasSize(capture.getImageWidth, capture.getImageHeight)
    // Exit the example when the canvas frame is closed
    canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    val delay = math.round(1000d / capture.getFrameRate)

    // Read frame by frame, stop early if the display window is closed
    for (i <- 0 until capture.getLengthInFrames; if canvasFrame.isVisible) {
        // Advance frame index
        capture.setFrameNumber(i)
        // Capture and show the frame
        canvasFrame.showImage(capture.grab())
        // Delay
        Thread.sleep(delay)
    }

    // Close the video file
    capture.release()
}
