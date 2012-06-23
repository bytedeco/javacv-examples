/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter08

import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_features2d._
import com.googlecode.javacv.cpp.opencv_nonfree.SURF
import java.io.File
import opencv2_cookbook.OpenCVUtils._


/**
 * Example for section "Describing SURF features" in chapter 8, page 212.
 *
 * There are a couple of tricky steps, in particular then sorting descriptors.
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
    val surfDesc = DescriptorExtractor.create("SURF").get
    val keyPoints = Array(new KeyPoint(), new KeyPoint())
    val descriptors = new Array[CvMat](2)

    // Detect SURF features and compute descriptors for both images
    for (i <- 0 to 1) {
        surf.detect(images(i), null, keyPoints(i))
        // Create non-empty CvMat, using simply `new CvMat()` leads to an exception.
        descriptors(i) = CvMat.create(1, 1)
        surfDesc.compute(images(i), keyPoints(i), descriptors(i))
    }

    // Create feature matcher
    val matcher = new BFMatcher(NORM_L2)

    val matches = new DMatch()
    matcher.`match`(descriptors(0), descriptors(1), matches, null)
    println("Matched: " + matches.capacity)

    // Select only 25 best matches
    val bestMatches = selectBest(matches, 25)

    // Reset position to 0 or there will be trouble while calling  `drawMatches`.
    // If you did something to other lists, like keyPoints, you will have to reset their `position` them too.
    bestMatches.position(0)

    // Draw best matches
    val imageMatches = cvCreateImage(new CvSize(images(0).width + images(1).width, images(0).height), images(0).depth, 3)
    drawMatches(images(0), keyPoints(0), images(1), keyPoints(1),
        bestMatches, imageMatches, CvScalar.BLUE, CvScalar.RED, null, DrawMatchesFlags.DEFAULT)
    show(imageMatches, "Best SURF Feature Matches")


    //----------------------------------------------------------------------------------------------------------------

    /**
     * Select only the best matches from the list. Return new list.
     */
    private def selectBest(matches: DMatch, numberToSelect: Int): DMatch = {
        // Convert to Scala collection for the sake of sorting
        val a = new Array[DMatch](matches.capacity())
        for (i <- 0 until a.size) {
            val src = matches.position(i)
            val dest = new DMatch()
            copy(src, dest)
            a(i) = dest
        }

        // Sort
        val aSorted = a.sortWith(_.compare(_))

        // Create new JavaCV list
        val best = new DMatch(numberToSelect)
        for (i <- 0 until numberToSelect) {
            // Since there is no may to `put` objects into a list DMatch,
            // We have to reassign all values individually, and hope that API will not any new ones.
            copy(aSorted(i), best.position(i))
        }

        best
    }

    private def copy(src: DMatch, dest: DMatch) {
        dest.distance(src.distance)
        dest.imgIdx(src.imgIdx)
        dest.queryIdx(src.queryIdx)
        dest.trainIdx(src.trainIdx)
    }
}
