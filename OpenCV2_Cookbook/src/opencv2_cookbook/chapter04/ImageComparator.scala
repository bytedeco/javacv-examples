/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import com.googlecode.javacv.cpp.opencv_core.IplImage
import com.googlecode.javacv.cpp.opencv_imgproc._


/**
 * Computes image similarity using `cvCompareHist`.
 */
class ImageComparator(val referenceImage: IplImage) {

    /**
     * Color reduction factor. The comparison will be made on images with
     * the color space reduced by this factor in each dimension
     */
    private val colorReductionFactor = 32
    private val hist = new ColorHistogram()

    ColorHistogram.colorReduce(referenceImage, colorReductionFactor)
    private val referenceHistogram = hist.getHistogram(referenceImage)


    /**
     * Compare the reference image with the given input image and return similarity score.
     */
    def compare(image: IplImage): Double = {
        ColorHistogram.colorReduce(image, colorReductionFactor)
        var inputH = hist.getHistogram(image)
        cvCompareHist(referenceHistogram, inputH, CV_COMP_INTERSECT)
    }
}
