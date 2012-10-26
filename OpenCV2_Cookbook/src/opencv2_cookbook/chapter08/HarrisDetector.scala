/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.awt.geom._
import java.awt.{Color, Graphics2D, Image, Point}


/** Uses Harris Corner strength image to detect well localized corners,
  * replacing several closely located detections (blurred) by a single one.
  *
  * Based on C++ class from chapter 8. Used by `Ex2HarrisCornerDetector`.
  */
class HarrisDetector {

    /** Neighborhood size for Harris edge detector. */
    var neighborhood = 3
    /** Aperture size for Harris edge detector. */
    var aperture = 3
    /** Harris parameter. */
    var k = 0.01

    /** Maximum strength for threshold computations. */
    var maxStrength = 0.0
    /** Size of kernel for non-max suppression. */
    var nonMaxSize = 3

    /** Image of corner strength, computed by Harris edge detector. It is created by method `detect()`. */
    private var cornerStrength: Option[IplImage] = None
    /** Image of local corner maxima. It is created by method `detect()`. */
    private var localMax: Option[IplImage] = None


    /** Compute Harris corners.
      *
      * Results of computation can be retrieved using `getCornerMap` and `getCorners`.
      */
    def detect(image: IplImage) {
        // Harris computations
        cornerStrength = Some(cvCreateImage(cvGetSize(image), IPL_DEPTH_32F, 1))
        cvCornerHarris(image, cornerStrength.get, neighborhood, aperture, k)

        // Internal threshold computation.
        //
        // We will scale corner threshold based on the maximum value in the cornerStrength image.
        // Call to cvMinMaxLoc finds min and max values in the image and assigns them to output parameters.
        // Passing back values through function parameter pointers works in C bout not on JVM.
        // We need to pass them as 1 element array, as a work around for pointers in C API.
        val maxStrengthA = Array(maxStrength)
        cvMinMaxLoc(
            cornerStrength.get,
            Array(0.0) /* not used here, but required by API */ ,
            maxStrengthA)
        // Read back the computed maxStrength
        maxStrength = maxStrengthA(0)

        // Local maxima detection.
        //
        // Dilation will replace values in the image by its largest neighbour value.
        // This process will modify all the pixels but the local maxima (and plateaus)
        val dilated = cvCreateImage(cvGetSize(cornerStrength.get), cornerStrength.get.depth, 1)
        cvDilate(cornerStrength.get, dilated, null, 1)
        localMax = Some(cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, 1))
        // Find maxima by detecting which pixels were not modified by dilation
        cvCmp(cornerStrength.get, dilated, localMax.get, CV_CMP_EQ)
    }


    /** Get the corner map from the computed Harris values. Require call to `detect`.
      * @throws IllegalStateException if `cornerStrength` and `localMax` are not yet computed.
      */
    def getCornerMap(qualityLevel: Double): IplImage = {
        if (cornerStrength.isEmpty || localMax.isEmpty) {
            throw new IllegalStateException("Need to call `detect()` before it is possible to compute corner map.")
        }

        // Threshold the corner strength
        val threshold = qualityLevel * maxStrength
        val cornerTh = cvCreateImage(cvGetSize(cornerStrength.get), cornerStrength.get.depth, 1)
        cvThreshold(cornerStrength.get, cornerTh, threshold, 255, CV_THRESH_BINARY)

        val cornerMap = cvCreateImage(cvGetSize(cornerTh), IPL_DEPTH_8U, 1)
        cvConvertScale(cornerTh, cornerMap, 1, 0)

        // non-maxima suppression
        cvAnd(cornerMap, localMax.get, cornerMap, null)

        cornerMap
    }


    /** Get the feature points from the computed Harris values. Require call to `detect`. */
    def getCorners(qualityLevel: Double): List[Point] = {
        // Get the corner map
        val cornerMap = getCornerMap(qualityLevel)
        // Get the corners
        getCorners(cornerMap)
    }


    /** Get the feature points vector from the computed corner map.  */
    private def getCorners(cornerMap: IplImage): List[Point] = {

        // Get access to image data on the JVM side
        val r = cornerMap.getBufferedImage.getRaster

        // Iterate over the pixels to obtain all feature points
        val width = r.getWidth
        val height = r.getHeight
        val points = for (y <- 0 until height; x <- 0 until width if r.getSample(x, y, 0) != 0) yield new Point(x, y)

        points.toList
    }


    /**
     * Draw circles at feature point locations on an image
     */
    def drawOnImage(image: IplImage, points: List[Point]): Image = {
        //        val radius = 3
        //        val thickness = 2
        //        points.foreach(p => {
        //            println("(" + p.x + ", " + p.y + ")")
        //            val center = new CvPoint(new CvPoint2D32f(p.x, p.y))
        //            cvCircle(image, center, radius, CvScalar.WHITE, thickness, 8, 0)
        //        })

        // OpenCV drawing seems to crash a lot, so use Java2D
        val radius = 3
        val bi = image.getBufferedImage
        val g2d = bi.getGraphics.asInstanceOf[Graphics2D]
        val w = radius * 2
        g2d.setColor(Color.WHITE)
        points.foreach(p => g2d.draw(new Ellipse2D.Double(p.x - radius, p.y - radius, w, w)))

        bi
    }
}