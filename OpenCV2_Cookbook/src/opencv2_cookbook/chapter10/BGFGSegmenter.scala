/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter10

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._

/** Moving average background/foreground segmenter.
  *
  * Described in section "Extracting the foreground objects in video", chapter 10.
  *
  * @param learningRate learning rate in background accumulation
  * @param threshold threshold for foreground extraction
  */
class BGFGSegmenter(val learningRate: Double = 0.01,
                    val threshold: Double = 10) {

    /** accumulated background */
    var background: IplImage = null

    /** Process single frame */
    def process(frame: IplImage): IplImage = {

        val output = cvCreateImage(cvGetSize(frame), frame.depth, 1)

        // convert to gray-level image
        val gray = cvCreateImage(cvGetSize(frame), frame.depth, 1)
        cvCvtColor(frame, gray, CV_BGR2GRAY)

        // initialize background to 1st frame
        if (background == null) {
            background = cvCreateImage(cvGetSize(frame), IPL_DEPTH_32F, 1)
            cvConvert(gray, background)
        }

        // convert background to 8U
        val backImage = cvCreateImage(cvGetSize(frame), IPL_DEPTH_8U, 1)
        cvConvert(background, backImage)

        // compute difference between current image and background
        val foreground = cvCreateImage(cvGetSize(frame), IPL_DEPTH_8U, 1)
        cvAbsDiff(backImage, gray, foreground)

        // apply threshold to foreground image
        cvThreshold(foreground, output, threshold, 255, CV_THRESH_BINARY_INV)

        // accumulate background
        cvRunningAvg(gray, background, learningRate, output)

        output
    }

}
