/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11


/** The first example for section "Extracting the foreground objects in video" in Chapter 10, page 272.
  *
  * Background / foreground segmentation is implemented in class `BGFGSegmenter`.
  *
  * Unlike in the C++ implementation, we do not need to pass `BGFGSegmenter` as a parameter to `VideoProcessor`,
  * since `VideoProcessor` need only access to method `process`. This simplifies implementation of `VideoProcessor`.
  * We can pass only the method `process` itself.
  * Scala will treat `process` as a "closure", that is method `process` will have access to all local member variables
  * in class `BGFGSegmenter`.
  */
object Ex5ForegroundSegmenter extends App {

  // Background / foreground segmenter.
  val segmenter = new BGFGSegmenter()

  // Create video processor instance
  val processor = new VideoProcessor()
  processor.input = "data/bike.avi"

  // Declare a window to display the video
  processor.displayInput = "Input Video"
  processor.displayOutput = "Output Video"
  // Play the video at the original frame rate
  processor.delay = math.round(1000d / processor.frameRate)
  // Set the frame processor callback function (pass BGFGSegmenter `process` method as a closure)
  processor.frameProcessor = segmenter.process

  // Start the process
  processor.run()

  println("Done.")
}
