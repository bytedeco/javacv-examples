/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter03

import java.awt.Color

/**
 * Represents a color in RGB color space. Component values are expected to be in range [0-255]
 */
case class ColorRGB(red: Int, green: Int, blue: Int) {

    def this(color: Color) = this(color.getRed, color.getGreen, color.getBlue)

    def toColor = new Color(red, green, blue)
}