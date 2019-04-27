/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11

import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/** Moving average background/foreground segmenter.
  *
  * Described in section "Extracting the foreground objects in video", chapter 10.
  *
  * @param learningRate learning rate in background accumulation
  * @param foregroundThreshold threshold for foreground extraction
  */
class BGFGSegmenter(val learningRate: Double = 0.01,
                    val foregroundThreshold: Double = 10) {

  /** accumulated background */
  private val background = new Mat()
  private val backImage  = new Mat()
  private val foreground = new Mat()

  /** Process single frame */
  def process(inputFrame: Mat, outputFrame: Mat): Unit = {

    // convert to gray-level image
    val gray = new Mat()
    cvtColor(inputFrame, gray, COLOR_BGR2GRAY)

    // initialize background to 1st frame
    if (background.empty()) {
      gray.convertTo(background, CV_32F)
    }

    // convert background to 8U
    background.convertTo(backImage, CV_8U)

    // compute difference between current image and background
    absdiff(backImage, gray, foreground)

    // apply threshold to foreground image
    threshold(foreground, outputFrame, foregroundThreshold, 255, THRESH_BINARY_INV)

    // accumulate background
    accumulateWeighted(gray,
      background, // alpha*gray + (1-alpha)*background
      learningRate, // alpha
      outputFrame // mask
    )

  }

}
