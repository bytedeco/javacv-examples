/*
 * Copyright (c) 2011-2012 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jarek.listy at gmail.com
 */

package opencv2_cookbook.chapter04

import opencv2_cookbook.OpenCVUtils._

import com.googlecode.javacv.cpp.opencv_highgui._

import java.io.File


/**
 * Example for section "Retrieving similar images using histogram comparison" in Chapter 4.
 */
object Ex9ImageComparator extends App {

    val referenceImageFile = new File("data/waves.jpg")

    val testImageFiles = Array(
        new File("data/waves.jpg"),
        new File("data/beach.jpg"),
        new File("data/dog.jpg"),
        new File("data/polar.jpg"),
        new File("data/bear.jpg"),
        new File("data/lake.jpg"),
        new File("data/moose.jpg")
    )

    // Load reference image
    val reference = loadOrExit(referenceImageFile, CV_LOAD_IMAGE_COLOR)

    // Setup comparator
    val comparator = new ImageComparator(reference)

    // Show reference image after color reduction done by `ImageComparator`
    show(comparator.referenceImage, "Reference")

    // Compute similarity for test images
    for (file <- testImageFiles) {
        val image = loadOrExit(file, CV_LOAD_IMAGE_COLOR)
        val imageSize = image.width * image.height
        // Compute histogram match and normalize by image size.
        // 1 means perfect match.
        val score = comparator.compare(image) / imageSize
        println(file.getName + ", score: %6.4f".format(score))
        show(image, file.getName + ", score: %6.4f".format(score))
    }
}
