/*
 * Copyright (c) 2011-2015 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter04

import java.io.File

import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_highgui._

/**
 * Creates inverted image by inverting its look-up table.
 * Example for section "Applying look-up table to modify image appearance" in Chapter 4.
 */
object Ex4InvertLut extends App {

  // Load image as a gray scale
  val src = loadAndShowOrExit(new File("data/group.jpg"), CV_LOAD_IMAGE_GRAYSCALE)

  // Create inverted lookup table
  val dim = 256
  val lut = new Mat(1, dim, CV_8U)
  val lutI = lut.createIndexer().asInstanceOf[UByteIndexer]
  for (i <- 0 until dim) {
    lutI.put(i, (dim - 1 - i).toByte)
  }

  // Apply look-up
  val dest = Histogram1D.applyLookUp(src, lut)

  // Show inverted image
  show(dest, "Inverted LUT")
}
