/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter03

import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_core.IplImage


/**
 * Implements the controller for the color corrector example in Chapter 3
 * sections "Using a controller to communicate with processing modules"
 * and "Using the Singleton design pattern".
 * The singleton pattern is easily created in Scala using an 'object' construct rather than 'class' construct.
 * `object` takes care of ensuring that only single instance is created, simplifying the source code.
 *
 * It uses Scala convention for setter and getters, that does not use `get*` and `set*` prefixes.
 * For instance, method `targetColor` is a getter for property `targetColor`,
 * method `targetColor_=` is setter and can be invoked using only assignment symbol `=`
 * {{{
 * var dcd = new ColorDetectorController
 * dcd.targetColor = new Color(130, 190, 230)
 * }}}
 */
object ColorDetectorController {

    private val colorDetector = new ColorDetector()

    /**
     * Image to be processed.
     */
    private var _inputImage: Option[IplImage] = None

    /**
     * Image result.
     */
    private var _result: Option[IplImage] = None


    /**
     * Get the color distance threshold.
     */
    def colorDistanceThreshold = colorDetector.colorDistanceThreshold


    /**
     * Set the color distance threshold.
     */
    def colorDistanceThreshold_=(dist: Int) {colorDetector.colorDistanceThreshold = dist}


    /**
     * Get the color to be detected
     */
    def targetColor = colorDetector.targetColor


    /**
     * Set the color to be detected
     */
    def targetColor_=(color: ColorRGB) {colorDetector.targetColor = color}


    /**
     * Get current input image
     */
    def inputImage: Option[IplImage] = _inputImage


    /**
     * Get result image, mau be `null`.
     */
    def result: Option[IplImage] = _result


    /**
     * Read the input image from a file. Return `true` if reading completed successfully.
     */
    def setInputImage(fileName: String): Boolean = {
        _inputImage = cvLoadImage(fileName, CV_LOAD_IMAGE_UNCHANGED) match {
            case null => None
            case x: IplImage => Some(x)
        }
        _inputImage != null
    }


    /**
     * Perform image processing.
     */
    def process() {
        require(_inputImage != null, "Input image not set yet.")
        _result = Some(colorDetector.process(_inputImage.get))
    }

}