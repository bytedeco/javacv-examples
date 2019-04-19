/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter04

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._


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
  val reference = loadOrExit(referenceImageFile, IMREAD_COLOR)

  // Setup comparator
  val comparator = new ImageComparator(reference)

  // Show reference image after color reduction done by `ImageComparator`
  show(comparator.referenceImage, "Reference")

  // Compute similarity for test images
  for (file <- testImageFiles) {
    val image = loadOrExit(file, IMREAD_COLOR)
    val imageSize = image.cols() * image.rows()
    // Compute histogram match and normalize by image size.
    // 1 means perfect match.
    val score = comparator.compare(image) / imageSize
    println(file.getName + ", score: %6.4f".format(score))
    show(image, file.getName + ", score: %6.4f".format(score))
  }
}
