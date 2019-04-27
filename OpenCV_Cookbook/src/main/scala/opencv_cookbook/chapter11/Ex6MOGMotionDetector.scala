/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11


import javax.swing.WindowConstants
import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameGrabber, OpenCVFrameConverter}
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.global.opencv_video._
import org.bytedeco.opencv.opencv_core._


/** The second example example for section "Extracting the foreground objects in video" in Chapter 10, page 272.
  *
  * This version of foreground segmentation is using a more elaborate approach based on modeling background as a
  * mixture of Gaussians. It is using OpenCV class `BackgroundSubtractorMOG`.
  *
  * This version of the example is implemented using OpenCV C API.
  *
  * @see opencv_cookbook.chapter11.Ex1ReadVideoSequence
  */
object Ex6MOGMotionDetector extends App {

  // Open video video file
  val grabber = new FFmpegFrameGrabber("data/bike.avi")
  // Open video video file
  grabber.start()

  // Prepare window to display frames
  val canvasFrame = new CanvasFrame("Extracted Foreground")
  canvasFrame.setCanvasSize(grabber.getImageWidth, grabber.getImageHeight)
  // Exit the example when the canvas frame is closed
  canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  // Time between frames in the video
  val delay = math.round(1000d / grabber.getFrameRate)

  // foreground binary image
  val foreground = new Mat()

  val frameConverter = new OpenCVFrameConverter.ToMat()

  // Mixture of Gaussians approach
  val mog = createBackgroundSubtractorMOG2()

  for (frame <- Iterator.continually(grabber.grab()).takeWhile(_ != null)) {

    val inputFrame = frameConverter.convert(frame)

    // update the background
    // and return the foreground
    mog(inputFrame, foreground, 0.01)

    // Complement the image
    threshold(foreground, foreground, 128, 255, THRESH_BINARY_INV)

    canvasFrame.showImage(toBufferedImage(foreground))

    // Delay
    Thread.sleep(delay)
  }

  grabber.release()
}
