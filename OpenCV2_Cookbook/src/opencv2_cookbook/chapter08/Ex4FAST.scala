/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_features2d.{KeyPoint, FastFeatureDetector}
import java.io.File
import opencv2_cookbook.OpenCVUtils._

/**
 * @author Jarek Sacha
 * @since 6/21/12 9:31 AM
 */
object Ex4FAST extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    // Detect fast features
    val ffd = new FastFeatureDetector(
        40 /* threshold for detection */ ,
        true /* non-max suppression */)
    val keyPoints = new KeyPoint()
    ffd.detect(image, keyPoints, null)

    // Draw keyPoints
    show(drawOnImage(image, asArray(keyPoints)), "Good Features to Track Detector")

    def asArray(keyPoints: KeyPoint): Array[KeyPoint] = {
        // Convert keyPoints to an array
        val n = keyPoints.capacity
        val points = new Array[KeyPoint](n)
        for (i <- 0 until n) {
            val p = new KeyPoint(keyPoints.position(i))
            points(i) = p
        }

        points
    }
}
