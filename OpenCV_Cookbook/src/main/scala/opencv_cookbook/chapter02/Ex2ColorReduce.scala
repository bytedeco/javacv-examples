/*
 * Copyright (c) 2011-2022 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter02

import opencv_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.indexer.{Indexer, OneIndex, UByteIndexer}
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.opencv_core._

import java.io.File

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

    // Total number of elements, combining components from each channel
    val nbElements = image.rows * image.cols * image.channels

    // Indexer is used to access value in the image
    val indexer =
      image
        .createIndexer()
        .asInstanceOf[Indexer] // cast needed to avoid: UByteRawIndexer cannot be cast to class scala.runtime.Nothing$
        .reindex[UByteIndexer](new OneIndex(nbElements)) // Reindex needed to avoid  IndexOutOfBoundsException later
    // See discussion: https://github.com/bytedeco/javacv-examples/issues/23

    for (i <- 0 until nbElements) {
      // Convert to integer, byte is treated as an unsigned value
      val v = indexer.get(i)
      // Use integer division to reduce number of values
      val newV = v / div * div + div / 2
      // Put back into the image
      indexer.put(i, newV)
    }

    image
  }
}
