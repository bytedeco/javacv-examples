/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import opencv2_cookbook.OpenCVUtils._
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_nonfree._
import java.io.File


/**
 * Example of extracting SIFT features from section "Detecting the scale-invariant SURF features" in chapter 8.
 */
object SIFT extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    val keyPoints = new KeyPoint()
    val nFeatures = 0
    val nOctaveLayers = 3
    val contrastThreshold = 0.04
    val edgeThreshold = 10
    val sigma = 1.6
    val sift = new SIFT(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma)
    sift.detect(image, null, keyPoints)

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