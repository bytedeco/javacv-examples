/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter10

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._


/** The example for section "Processing the video frames" in Chapter 10, page 251.
  *
  * Frames are read from a video file and displayed using `VideoProcessor` class.
  * Each frame is processed to detect edges using `canny` that is passed as
  * frame processing method to the `VideoProcessor`.
  */
object Ex2ProcessVideoFrames extends App {

  /** Method for processing video frames, that is passed to `VideoProcessor`. */
  def canny(src: IplImage): IplImage = {
    val dest = cvCreateImage(cvGetSize(src), src.depth(), 1)
    // Convert to gray
    cvCvtColor(src, dest, CV_BGR2GRAY)
    // Compute Canny edges
    cvCanny(dest, dest, 100, 200, 3)
    // Invert the image
    cvThreshold(dest, dest, 128, 255, CV_THRESH_BINARY_INV)
    dest
  }


  // Open video file
  val capture = cvCreateFileCapture("data/bike.avi")

  // Create video processor instance
  val processor = new VideoProcessor(capture)
  // Declare a window to display the input and output video
  processor.displayInput = "Input Video"
  processor.displayOutput = "Output Video"
  // Play the video at the original frame rate
  processor.delay = math.round(1000d / processor.frameRate)
  // Set the frame processing method
  processor.frameProcessor = canny

  // Start the processing loop
  processor.run()

  // Close the video file
  cvReleaseCapture(capture)
}
