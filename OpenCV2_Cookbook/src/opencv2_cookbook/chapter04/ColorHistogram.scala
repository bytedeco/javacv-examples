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
 * Companion methods for `ColorHistogram`.
 */
object ColorHistogram {
    /**
     * Reduce number of colors, described in OpenCV Cookbook Chapter 2.
     * @param image input image that will have colors modified after this call.
     * @param div color reduction factor.
     */
    def colorReduce(image: IplImage, div: Int = 64) {

        val mat = toCvMat(image)

        // Total number of elements, combining components from each channel
        val nbElements = mat.rows * mat.cols * mat.channels
        for (i <- 0 until nbElements) {
            // Convert to integer
            val v = mat.get(i).toInt
            // Use integer division to reduce number of values
            val newV = v / div * div + div / 2
            // Put back into the image
            mat.put(i, newV)
        }
    }

    /**
     * Split channels in a 3 channel image, for instance, color image.
     * @param src 3 channel image
     * @return array of 3 channels
     */
    def splitChannels(src: IplImage): Array[IplImage] = {
        require(src != null, "Argument `src` cannot be null.")
        require(src.nChannels == 3, "Expecting 3 channel (color) image")

        val size = cvGetSize(src)
        val channel0 = cvCreateImage(size, src.depth, 1)
        val channel1 = cvCreateImage(size, src.depth, 1)
        val channel2 = cvCreateImage(size, src.depth, 1)
        cvSplit(src, channel0, channel1, channel2, null)

        Array(channel0, channel1, channel2)
    }
}

/**
 * Helper class that simplifies usage of OpenCV `cv::calcHist` function for color images.
 *
 * See OpenCV [[http://opencv.itseez.com/modules/imgproc/doc/histograms.html?highlight=histogram]]
 * documentation to learn backend details.
 */
class ColorHistogram {

    import ColorHistogram._

    val numberOfBins = 256
    private var _minRange = 0.0f
    private var _maxRange = 255.0f

    /**
     * Computes histogram of an image.
     * Returned CvHistogram object has to be manually deallocated after use using `cvReleaseHist`.
     *
     * @param image input image
     * @return OpenCV histogram object
     */
    def getHistogram(image: IplImage): CvHistogram = {
        require(image != null)
        require(image.nChannels == 3, "Expecting 3 channel (color) image")

        // Allocate histogram object
        val dims = 3
        val sizes = Array(numberOfBins, numberOfBins, numberOfBins)
        val histType = CV_HIST_SPARSE
        val minMax = Array(_minRange, _maxRange)
        val ranges = Array(minMax, minMax, minMax)
        val uniform = 1
        val hist = cvCreateHist(dims, sizes, histType, ranges, uniform)

        // Split bands, as required by `cvCalcHist`
        val channel0 = cvCreateImage(cvGetSize(image), image.depth, 1)
        val channel1 = cvCreateImage(cvGetSize(image), image.depth, 1)
        val channel2 = cvCreateImage(cvGetSize(image), image.depth, 1)
        cvSplit(image, channel0, channel1, channel2, null)

        // Compute histogram
        val accumulate = 0
        val mask = null
        cvCalcHist(Array(channel0, channel1, channel2), hist, accumulate, mask)
        hist
    }

    /**
     * Convert input image from RGB ro HSV color space and compute histogram of the hue channel.
     * @param image RGB image
     * @param minSaturation minimum saturation of pixels that are used for histogram calculations.
     *                      Pixels with saturation larger than minimum will be used in histogram computation
     * @return histogram of the hue channel, its range is from 0 to 180.
     */
    def getHueHistogram(image: IplImage, minSaturation: Int = 0): CvHistogram = {
        require(image != null)
        require(image.nChannels == 3, "Expecting 3 channel (color) image")

        // Convert RGB to HSV color space
        val hsvImage = cvCreateImage(cvGetSize(image), image.depth, 3)
        cvCvtColor(image, hsvImage, CV_BGR2HSV)

        // Split the 3 channels into 3 images
        val hsvChannels = splitChannels(hsvImage)

        val saturationMask = if (minSaturation > 0) {
            val saturationMask = cvCreateImage(cvGetSize(hsvImage), IPL_DEPTH_8U, 1)
            cvThreshold(hsvChannels(1), saturationMask, minSaturation, 255, CV_THRESH_BINARY)
            saturationMask
        } else {
            null
        }

        // Compute histogram of the hue channel
        val h1D = new Histogram1D()
        h1D.setRanges(0, 180)
        h1D.getHistogram(hsvChannels(0), saturationMask)
    }


}