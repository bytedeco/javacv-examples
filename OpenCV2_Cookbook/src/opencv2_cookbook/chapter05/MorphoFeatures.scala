/*
 * Copyright (c) 2011 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter05

import com.googlecode.javacv.cpp.opencv_imgproc._
import com.googlecode.javacv.cpp.opencv_core._
import java.awt.{Color, Graphics2D, Image}
import java.awt.geom.Ellipse2D


/**
 * Equivalent of C++ class MorphoFeatures presented in section "Detecting edges and filters using
 * morphological filters". Contains methods for morphological corner detection.
 */
class MorphoFeatures {

    // Threshold to produce binary image
    var threshold = -1

    // Structural elements used in corner detection
    private val cross = cvCreateStructuringElementEx(5, 5, 2, 2, CV_SHAPE_CUSTOM,
        Array(
            0, 0, 1, 0, 0,
            0, 0, 1, 0, 0,
            1, 1, 1, 1, 1,
            0, 0, 1, 0, 0,
            0, 0, 1, 0, 0
        )
    )
    private val diamond = cvCreateStructuringElementEx(5, 5, 2, 2, CV_SHAPE_CUSTOM,
        Array(
            0, 0, 1, 0, 0,
            0, 1, 1, 1, 0,
            1, 1, 1, 1, 1,
            0, 1, 1, 1, 0,
            0, 0, 1, 0, 0
        )
    )
    private val square = cvCreateStructuringElementEx(5, 5, 2, 2, CV_SHAPE_RECT, null)
    private val x = cvCreateStructuringElementEx(5, 5, 2, 2, CV_SHAPE_CUSTOM,
        Array(
            1, 0, 0, 0, 1,
            0, 1, 0, 1, 0,
            0, 0, 1, 0, 0,
            0, 1, 0, 1, 0,
            1, 0, 0, 0, 1
        )
    )


    def getEdges(image: IplImage): IplImage = {
        // Get gradient image
        val result = cvCreateImage(cvGetSize(image), image.depth, 1)
        val element3 = cvCreateStructuringElementEx(3, 3, 1, 1, CV_SHAPE_RECT, null)
        cvMorphologyEx(image, result, null, element3, MORPH_GRADIENT, 1)

        // Apply threshold to obtain a binary image
        applyThreshold(result)

        result
    }


    def getCorners(image: IplImage): IplImage = {
        val result = cvCreateImage(cvGetSize(image), image.depth, 1)

        // Dilate with a cross
        cvDilate(image, result, cross, 1)

        // Erode with a diamond
        cvErode(result, result, diamond, 1)

        val result2 = cvCreateImage(cvGetSize(image), image.depth, 1)
        // Dilate with X
        cvDilate(image, result2, x, 1)

        // Erode with a square
        cvErode(result2, result2, square, 1)

        // Corners are obtained by differentiating the two closed images
        cvAbsDiff(result2, result, result)

        // Apply threshold to get binary image
        applyThreshold(result)

        result
    }


    private def applyThreshold(image: IplImage) {
        if (threshold > 0) {
            cvThreshold(image, image, threshold, 255, CV_THRESH_BINARY_INV)
        }
    }


    /**
     * Draw circles at feature point locations on an image it assumes that images are of the same size.
     */
    def drawOnImage(binary: IplImage, image: IplImage): Image = {

        // OpenCV drawing seems to crash a lot, so use Java2D
        val binaryRaster = binary.getBufferedImage.getData
        val radius = 3
        val diameter = radius * 2

        val imageBI = image.getBufferedImage
        val width = imageBI.getWidth
        val height = imageBI.getHeight
        val g2d = imageBI.getGraphics.asInstanceOf[Graphics2D]
        g2d.setColor(Color.WHITE)

        for (y <- 0 until height) {
            for (x <- 0 until width) {
                val v = binaryRaster.getSample(x, y, 0);
                if (v == 0) {
                    g2d.draw(new Ellipse2D.Double(x - radius, y - radius, diameter, diameter))
                }
            }
        }

        imageBI
    }

}
