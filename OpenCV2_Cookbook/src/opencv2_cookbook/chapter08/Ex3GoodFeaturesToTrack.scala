/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_features2d.{KeyPoint, GFTTDetector}
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Example of using the Good Features to Track detector.
 *
 * The third example for section "Detecting Harris corners" in Chapter 8, page 202.
 */
object Ex3GoodFeaturesToTrack extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    // Compute good features to track
    val gftt = new GFTTDetector(
        500 /* maximum number of corners to be returned */ ,
        0.01 /* quality level*/ ,
        10.0 /* minimum allowed distance between points*/ ,
        3 /* block size*/ ,
        false /* use Harris detector*/ ,
        0.04 /* Harris parameter */
    )
    val keyPoints = new KeyPoint()
    gftt.detect(image, keyPoints, null)

    // Draw keyPoints
    show(drawOnImage(image, keyPoints), "Good Features to Track Detector")
}