/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_nonfree._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Example of extracting SURF features from section "Detecting the scale-invariant SURF features" in chapter 8.
 */
object Ex5SURF extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    // Detect SURF features.
    val keyPoints = new KeyPoint()
    val hessianThreshold = 2500d
    val nOctaves = 4
    val nOctaveLayers = 2
    val extended = true
    val upright = false
    val surf = new SURF(hessianThreshold, nOctaves, nOctaveLayers, extended, upright)
    surf.detect(image, null, keyPoints)

    // Draw keyPoints
    val featureImage = IplImage.create(cvGetSize(image), image.depth(), 3)
    drawKeypoints(image, keyPoints, featureImage, CvScalar.WHITE, DrawMatchesFlags.DRAW_RICH_KEYPOINTS)
    show(featureImage, "SURF Features")
}