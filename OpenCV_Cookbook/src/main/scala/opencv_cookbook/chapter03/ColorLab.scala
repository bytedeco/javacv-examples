/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter03

/**
 * Represents a color in L*a*b* color space. Color values are expected to be in usual L*a*b* range:
 * L* 0 to 100,
 * a* -100 to 100,
 * b* -100 to 100.
 *
 * The color component values can be also read as UInt8 numbers the way OpenCV encodes UInt8 L*a*b* values:
 * L <- L*255/100,
 * a <- a + 128,
 * b <- b + 128.
 * See [[http://opencv.itseez.com/modules/imgproc/doc/miscellaneous_transformations.html?highlight=cvtcolor#void%20cvCvtColor(const%20CvArr*%20src,%20CvArr*%20dst,%20int%20code)]] documentation.
 */
case class ColorLab(l: Double, a: Double, b: Double) {
  def lAsUInt8: Int = (l * 255 / 100).toInt

  def aAsUInt8: Int = (a + 128).toInt

  def bAsUInt8: Int = (b + 128).toInt
}