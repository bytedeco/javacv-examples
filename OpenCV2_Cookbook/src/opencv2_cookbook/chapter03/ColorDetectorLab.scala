/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter03

import opencv2_cookbook.OpenCVImageJUtils._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._
import ij.process.ByteProcessor
import java.awt.Color
import math._


/**
 * Example of using a strategy pattern in algorithm design.
 * The pattern encapsulates an algorithm into a separate class.
 * To run this example use [[opencv2_cookbook.chapter03.Ex1ColorDetector]].
 *
 * The algorithm converts the input image to a binary by checking is pixel color is within a given distance from a desired color.
 * Pixels with color close to the desired color are white, other black.
 * Image is first converted from RGB to L*a*b* color space. Distance is computed in L*a*b*.
 *
 * This Scala code functionally is equivalent to C++ code in chapter 3 section
 * "Converting color spaces".
 * To make operations on image pixels easier and more efficient
 * OpenCV color image is converted to ImageJ representation during processing.
 *
 * Unlike the in the C++ example, this class does not pre-allocates and hold space for process image,
 * it is create only when needed.
 */
class ColorDetectorLab(private var _minDist: Int = 30,
                       // Need to remember that Color is interpreted here as L*a*b* scaled to (0-255), rather than RGB
                       // It as also stored as (b*, a*, L*)
                       private var _targetLab: ColorLab = ColorLab(74, -9, -26)) {


    def colorDistanceThreshold = _minDist

    def colorDistanceThreshold_=(dist: Int) {_minDist = max(0, dist)}

    def targetColor = _targetLab

    def targetColor_=(color: ColorLab) {_targetLab = color}

    def process(rgbImage: IplImage): IplImage = {

        // Convert input from RGB to L*a*b* color space
        // Note that since destination image uses 8 bit unsigned integers, original L*a*b* values
        // are converted to fit 0-255 range
        //       L <- L*255/100
        //       a <- a + 128
        //       b <- b + 128
        val labImage = cvCreateImage(cvGetSize(rgbImage), rgbImage.depth, 3)
        cvCvtColor(rgbImage, labImage, CV_BGR2Lab)

        // Convert to ColorProcessor for easier pixel access
        val src = toColorProcessor(labImage)

        // Create output image
        val dest = new ByteProcessor(src.getWidth, src.getHeight)

        // Iterate through pixels and check if their distance from the target color is
        // withing the distance threshold, if it is set `dest` to 255
        for (y <- 0 until src.getHeight) {
            for (x <- 0 until src.getWidth) {
                // Need to remember that now Color is interpreted as L*a*b* scaled to (0-255), rather than RGB
                // though distance calculation here work the same as for RGB
                if (distance(src.getColor(x, y)) < _minDist) {
                    dest.set(x, y, 255)
                }
            }
        }

        // Convert back to IplImage
        IplImage.createFrom(toBufferedImage(dest))
    }

    @inline
    private def distance(color: Color): Double = {
        // When converting to 8-bit representation L* is scaled, a* and b* are only shifted.
        // To make the distance calculations more proportional we scale here L* difference back.
        abs(_targetLab.bAsUInt8 - color.getRed) +
                abs(_targetLab.aAsUInt8 - color.getGreen) +
                abs(_targetLab.lAsUInt8 - color.getBlue) / 255d * 100d
    }
}