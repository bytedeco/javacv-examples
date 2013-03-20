/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter02

import com.googlecode.javacv.cpp.opencv_core.CvMat
import com.googlecode.javacv.cpp.opencv_highgui._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Reduce colors in the image by modifying color values in all bands the same way.
 *
 * Illustrates access to pixel values using absolute indexing.
 */
object Ex2ColorReduce extends App {

    // Read input image
    val image = loadMatAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

    // Add salt noise
    val dest = colorReduce(image)

    // Display
    show(dest, "Reduced colors")


    /**
     * Reduce number of colors.
     * @param image input image.
     * @param div color reduction factor.
     */
    def colorReduce(image: CvMat, div: Int = 64): CvMat = {

        // Total number of elements, combining components from each channel
        val nbElements = image.rows * image.cols * image.channels
        for (i <- 0 until nbElements) {
            // Convert to integer
            val v = image.get(i).toInt
            // Use integer division to reduce number of values
            val newV = v / div * div + div / 2
            // Put back into the image
            image.put(i, newV)
        }

        image
    }
}