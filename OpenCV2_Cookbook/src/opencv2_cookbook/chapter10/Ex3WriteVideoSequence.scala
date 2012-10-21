/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter10

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File


/** The example for section "Writing video sequences" in Chapter 10, page 261.
  *
  * Frames are read from a video file, processed, and saved to a new video file using `VideoProcessor` class.
  * Each frame is processed to detect edges using `canny` that is passed as
  * frame processing method to the `VideoProcessor`.
  */
object Ex3WriteVideoSequence extends App {

    // Define a method for processing video frames
    def canny(src: IplImage): IplImage = {
        val dest = cvCreateImage(cvGetSize(src), src.depth(), 1)
        // Convert to gray
        cvCvtColor(src, dest, CV_BGR2GRAY)
        // Compute Canny edges
        cvCanny(dest, dest, 100, 200, 3)
        // Invert the image
        cvThreshold(dest, dest, 128, 255, CV_THRESH_BINARY_INV)
        // Indicate processing progress
        print(".")
        dest
    }


    val file = new File("data/bike.avi")
    println("Processing video file: " + file.getAbsolutePath)

    // Open video file
    val capture = cvCreateFileCapture(file.getAbsolutePath)

    // Create video processor instance
    val processor = new VideoProcessor(capture)
    // Do not display video while processing
    processor.displayInput = ""
    processor.displayOutput = ""
    // Play the video at the original frame rate
    processor.delay = 0
    // Set the frame processor callback function
    processor.frameProcessor = canny

    // Decide which codec to use for output video
    val codec = if (System.getProperty("os.name").toLowerCase.startsWith("windows")) {
        CV_FOURCC_PROMPT // prompt used with list of available codecs
    } else {
        0 // Use the same is input
    }
    // Indicate file name and coded to use to write video
    processor.setOutput("../bikeOut.avi", codec = codec)

    // Start the process
    processor.run()

    // Close the video file
    cvReleaseCapture(capture)
    println("\nVideo procesing done.")
}
