/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import java.io.File
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_features2d.SIFT.CommonParams._
import opencv2_cookbook.OpenCVUtils._


/**
 * Example of extracting SIFT features from section "Detecting the scale-invariant SURF features" in chapter 8.
 */
object SIFT extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    val keyPoints = new KeyPoint()
    val sift = new SiftFeatureDetector(
        0.03 /* */ ,
        10.0 /* */ ,
        DEFAULT_NOCTAVES,
        DEFAULT_NOCTAVE_LAYERS,
        DEFAULT_FIRST_OCTAVE,
        FIRST_ANGLE
    )
    sift.detect(image, keyPoints, null)

    System.out.println("keyPoints: " + keyPoints.capacity)

    // Convert keyPoints to an array
    val n = keyPoints.capacity
    val points = new Array[KeyPoint](n)
    for (i <- 0 until n) {
        val p = new KeyPoint(keyPoints.position(i))
        points(i) = p
    }

    // Draw keyPoints
    show(drawOnImage(image, points), "Key Points")
}