/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter03

import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._


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
  private var _inputImage: Option[Mat] = None

  /**
   * Image result.
   */
  private var _result: Option[Mat] = None


  /**
   * Get the color distance threshold.
   */
  def colorDistanceThreshold: Int = colorDetector.colorDistanceThreshold


  /**
   * Set the color distance threshold.
   */
  def colorDistanceThreshold_=(dist: Int) {
    colorDetector.colorDistanceThreshold = dist
  }


  /**
   * Get the color to be detected
   */
  def targetColor: ColorRGB = colorDetector.targetColor


  /**
   * Set the color to be detected
   */
  def targetColor_=(color: ColorRGB) {
    colorDetector.targetColor = color
  }


  /**
   * Get current input image
   */
  def inputImage: Option[Mat] = _inputImage


  /**
   * Get result image, mau be `null`.
   */
  def result: Option[Mat] = _result


  /**
   * Read the input image from a file. Return `true` if reading completed successfully.
   */
  def setInputImage(fileName: String): Boolean = {
    _inputImage = imread(fileName, IMREAD_UNCHANGED) match {
      case null => None
      case x: Mat => Some(x)
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