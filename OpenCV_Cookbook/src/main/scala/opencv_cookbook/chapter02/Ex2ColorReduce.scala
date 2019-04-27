/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter02

import java.io.File

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._


/**
 * Reduce colors in the image by modifying color values in all bands the same way.
 *
 * Illustrates access to pixel values using absolute indexing.
 */
object Ex2ColorReduce extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_COLOR)

  // Add salt noise
  val dest = colorReduce(image)

  // Display
  show(dest, "Reduced colors")


  /**
   * Reduce number of colors.
   * @param image input image.
   * @param div color reduction factor.
   */
  def colorReduce(image: Mat, div: Int = 64): Mat = {

    // Indexer is used to access value in the image
    val indexer = image.createIndexer().asInstanceOf[UByteIndexer]

    // Total number of elements, combining components from each channel
    val nbElements = image.rows * image.cols * image.channels
    for (i <- 0 until nbElements) {
      // Convert to integer, byte is treated as an unsigned value
      val v = indexer.get(i) & 0xFF
      // Use integer division to reduce number of values
      val newV = v / div * div + div / 2
      // Put back into the image
      indexer.put(i, (newV & 0xFF).toByte)
    }

    image
  }
}