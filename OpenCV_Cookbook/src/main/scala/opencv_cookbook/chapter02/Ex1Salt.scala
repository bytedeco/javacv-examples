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

import scala.util.Random

/**
  * Demonstrates use of `ByteIndexer` to set individual, randomly selected, pixels to a fixed value.
  *
  * Illustrates access to pixel values using absolute indexing.
  */
object Ex1Salt extends App {

  // Read input image
  val image = loadAndShowOrExit(new File("data/boldt.jpg"), IMREAD_COLOR)

  // Add salt noise
  val dest = salt(image, 2000)

  // Display
  show(dest, "Salted")


  /**
    * Add 'salt' noise.
    *
    * @param image input image.
    * @param n     number of 'salt' grains.
    */
  def salt(image: Mat, n: Int): Mat = {

    // Random number generator
    val random = new Random()

    // Get access to image data
    val indexer = image.createIndexer().asInstanceOf[UByteIndexer]

    // Place `n` grains at random locations
    val nbChannels = image.channels
    for (_ <- 1 to n) {
      // Create random index of a pixel
      val row: Long = random.nextInt(image.rows)
      val col: Long = random.nextInt(image.cols)
      // Set it to white by setting each of the channels to max (255)
      for (i <- 0 until nbChannels) {
        indexer.put(row, col, i.toLong, 255.toByte)
      }
    }

    image
  }
}