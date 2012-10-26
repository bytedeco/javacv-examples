/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter10

import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_video.BackgroundSubtractorMOG
import javax.swing.JFrame


/** The second example example for section "Extracting the foreground objects in video" in Chapter 10, page 272.
  *
  * This version of foreground segmentation is using a more elaborate approach based on modeling background as a
  * mixture of Gaussians. It is using OpenCV class `BackgroundSubtractorMOG`.
  *
  * This version of the example is implemented using OpenCV C API.
  *
  * @see opencv2_cookbook.chapter10.Ex1ReadVideoSequenceJavaCV
  */
object Ex6MOGMotionDetector extends App {

    // Open video video file
    val capture = cvCreateFileCapture("data/bike.avi")

    // Prepare window to display frames
    val canvasFrame = new CanvasFrame("Extracted Foreground")
    val width = cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH).toInt
    val height = cvGetCaptureProperty(capture, CV_CAP_PROP_FRAME_HEIGHT).toInt
    canvasFrame.setCanvasSize(width, height)
    canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    // Time between frames in the video
    val delay = math.round(1000d / cvGetCaptureProperty(capture, CV_CAP_PROP_FPS))

    // Mixture of Gaussians approach
    val mog = new BackgroundSubtractorMOG()


    // Read frame by frame, stop early if the display window is closed
    var frame: IplImage = null
    while (cvGrabFrame(capture) != 0 && {frame = cvRetrieveFrame(capture); frame} != null) {

        // Update the mixture of Gaussians model with current frame and return estimated foreground
        val foreground = cvCreateImage(cvGetSize(frame), IPL_DEPTH_8U, 1)
        mog(frame, foreground, 0.01)

        // Complement the image, so foreground is black
        cvThreshold(foreground, foreground, 128, 255, CV_THRESH_BINARY_INV)

        canvasFrame.showImage(foreground)

        // Delay
        Thread.sleep(delay)
    }

    // Close the video file
    cvReleaseCapture(capture)
}
