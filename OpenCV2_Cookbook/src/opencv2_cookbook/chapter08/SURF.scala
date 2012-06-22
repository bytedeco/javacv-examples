/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_nonfree._
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Example of extracting SURF features from section "Detecting the scale-invariant SURF features" in chapter 8.
 */
object SURF extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    val keyPoints = new KeyPoint()
    val hessianThreshold = 2500d
    val nOctaves = 4
    val nOctaveLayers = 2
    val extended = false
    val upright = false
    val surf = new SURF(hessianThreshold, nOctaves, nOctaveLayers, extended, upright)
    surf.detect(image, null, keyPoints)

    // Draw keyPoints
    show(drawOnImage(image, keyPoints), "Key Points")
}