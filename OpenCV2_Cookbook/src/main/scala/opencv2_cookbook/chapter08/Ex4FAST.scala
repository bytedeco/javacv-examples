/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_features2d.{KeyPoint, FastFeatureDetector}
import java.io.File
import opencv2_cookbook.OpenCVUtils._

/**
 * The example for section "Detecting FAST features" in Chapter 8, page 203.
 */
object Ex4FAST extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    // Detect FAST features
    val ffd = new FastFeatureDetector(
        40 /* threshold for detection */ ,
        true /* non-max suppression */)
    val keyPoints = new KeyPoint()
    ffd.detect(image, keyPoints, null)

    // Draw keyPoints
    show(drawOnImage(image, keyPoints), "FAST Features")
}
