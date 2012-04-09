/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import opencv2_cookbook.OpenCVUtils._
import java.io.File
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_core._


/**
 * Example of extracting SURF features from section "Detecting the scale-invariant SURF features" in chapter 8.
 * This version is using "C API", original example in the book is using "C++ API".
 */
object SURF_c extends App {

    // Read input image
    val image = loadAndShowOrExit(new File("data/church01.jpg"))

    // Setup SURF
    val keyPoints = new CvSeq()
    val descriptors = new CvSeq()
    var storage = cvCreateMemStorage(0)
    var params = cvSURFParams(500, 1)

    // Run SURF on the input image
    cvExtractSURF(
        image /* image */ ,
        null /* mask */ ,
        keyPoints,
        descriptors,
        storage,
        params,
        0
    )

    // Display results
    val keyPointsArray = castToKeyPoints(keyPoints)
    show(drawOnImage(image, keyPointsArray), "Key Points")


    /**
     * Convert OpenCV sequence of KeyPoints to ann Scala array of KeyPoints.
     */
    def castToKeyPoints(seq: CvSeq): Array[KeyPoint] = {
        val n = seq.total()
        val points = new Array[KeyPoint](n)
        for (i <- 0 until n) {
            val p = new KeyPoint(cvGetSeqElem(keyPoints, i))
            points(i) = p
        }

        points
    }
}