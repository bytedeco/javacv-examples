/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._

/**
 * Used by examples from section "Backprojecting a histogram to detect specific image content" in chapter 4.
 * There are some differences in implementation.
 * The original OpenCV2 Cookbook examples use C++ API for histogram operations that are not available in JavaCV.
 * In particular, the histogram representations and the C++ `cv::calcBackProject` (not available in JavaCV)
 * takes additional arguments that are not available in `cvCalcBackProject`, like `channels`, `ranges`, `scale`, `uniform`.
 */
class ContentFinder {
    private var _threshold = -1f
    private var _histogram: CvHistogram = null;

    def threshold: Float = _threshold

    /**
     * Set threshold for converting the back-projected image to a binary.
     * If value is negative no thresholding will be done.
     */
    def threshold_=(t: Float) {_threshold = t}

    def histogram: CvHistogram = _histogram

    /**
     * Set reference histogram, it will be normalized.
     */
    def histogram_=(h: CvHistogram) {
        _histogram = h
        cvNormalizeHist(h, 1)
    }

    /**
     * Find content back projecting a histogram.
     * @param image input used for back projection.
     * @return Result of the back-projection of the histogram. Image is binary (0,255) if threshold is larger than 0.
     *         The returned image depth is `IPL_DEPTH_8U`.
     */
    def find(image: IplImage): IplImage = {

        // Split the input image into channels, each image passed to `cvCalcBackProject` must be single channel.
        // Convert each channel image to a to 32 bit floating point image.
        val channels = ColorHistogram.splitChannels(image) map toIplImage32F

        // Back project
        val dest = cvCreateImage(cvGetSize(image), IPL_DEPTH_32F, 1)
        cvCalcBackProject(channels, dest, histogram)

        if (threshold > 0) {
            cvThreshold(dest, dest, threshold, 1, CV_THRESH_BINARY)
        }

        toIplImage8U(dest)
    }
}
