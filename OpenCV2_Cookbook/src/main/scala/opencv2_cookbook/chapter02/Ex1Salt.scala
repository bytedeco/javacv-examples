/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv2_cookbook.chapter02

import java.io.File
import opencv2_cookbook.OpenCVUtils._
import org.bytedeco.javacpp.opencv_core.CvMat
import org.bytedeco.javacpp.opencv_highgui._
import util.Random

/**
 * Set individual, randomly selected, pixels to a fixed value.
 *
 * Illustrates access to pixel values using absolute indexing.
 */
object Ex1Salt extends App {

  // Read input image
  val image = loadCvMatAndShowOrExit(new File("data/boldt.jpg"), CV_LOAD_IMAGE_COLOR)

  // Add salt noise
  val dest = salt(image, 2000)

  // Display
  show(dest, "Salted")

  /**
   * Add 'salt' noise.
   * @param image input image.
   * @param n number of 'salt' grains.
   */
  def salt(image: CvMat, n: Int): CvMat = {

    // Place 'n' white spots at random locations
    val size = image.rows * image.cols
    val nbChannels = image.channels
    val random = new Random
    for (i <- 1 to n) {
      // Create random index of a pixel
      val index = random.nextInt(size)
      val offset = index * nbChannels
      // Set it to white by setting each of the channels to max (255)
      for (i <- 0 until nbChannels) {
        image.put(offset + i, 255)
      }
    }

    image
  }
}