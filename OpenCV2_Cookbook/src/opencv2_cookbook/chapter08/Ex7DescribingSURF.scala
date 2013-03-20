/*
 * Copyright (c) 2011-2013 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_nonfree.SURF
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/** Example for section "Describing SURF features" in chapter 8, page 212.
  *
  * Computes SURF features,  extracts their descriptors, and finds best matching descriptors between two images of the same object.
  * There are a couple of tricky steps, in particular sorting the descriptors.
  */
object Ex7DescribingSURF extends App {

    // Read input image
    val images = Array(
        loadAndShowOrExit(new File("data/church01.jpg")),
        loadAndShowOrExit(new File("data/church02.jpg"))
    )

    // Setup SURF feature detector and descriptor.
    val hessianThreshold = 2500d
    val nOctaves = 4
    val nOctaveLayers = 2
    val extended = true
    val upright = false
    val surf = new SURF(hessianThreshold, nOctaves, nOctaveLayers, extended, upright)
    val surfDesc = DescriptorExtractor.create("SURF")
    val keyPoints = Array(new KeyPoint(), new KeyPoint())
    val descriptors = new Array[CvMat](2)

    // Detect SURF features and compute descriptors for both images
    for (i <- 0 to 1) {
        surf.detect(images(i), null, keyPoints(i))
        // Create CvMat initialized with empty pointer, using simply `new CvMat()` leads to an exception.
        descriptors(i) = new CvMat(null)
        surfDesc.compute(images(i), keyPoints(i), descriptors(i))
    }

    // Create feature matcher
    val matcher = new BFMatcher(NORM_L2, false)

    val matches = new DMatch()
    // "match" is a keyword in Scala, to avoid conflict between a keyword and a method match of the BFMatcher,
    // we need to enclose method name in ticks: `match`.
    matcher.`match`(descriptors(0), descriptors(1), matches, null)
    println("Matched: " + matches.capacity)

    // Select only 25 best matches
    val bestMatches = selectBest(matches, 25)

    // Draw best matches
    val imageMatches = cvCreateImage(new CvSize(images(0).width + images(1).width, images(0).height), images(0).depth, 3)
    drawMatches(images(0), keyPoints(0), images(1), keyPoints(1),
        bestMatches, imageMatches, CvScalar.BLUE, CvScalar.RED, null, DrawMatchesFlags.DEFAULT)
    show(imageMatches, "Best SURF Feature Matches")


    //----------------------------------------------------------------------------------------------------------------


    /** Select only the best matches from the list. Return new list. */
    private def selectBest(matches: DMatch, numberToSelect: Int): DMatch = {
        // Convert to Scala collection, and sort
        val sorted = toArray(matches).sortWith(_ compare _)

        // Select the best, and return in native vector
        toNativeVector(sorted.take(numberToSelect))
    }
}
