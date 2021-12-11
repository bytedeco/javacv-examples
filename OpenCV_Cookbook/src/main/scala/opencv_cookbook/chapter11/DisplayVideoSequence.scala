/*
 * Copyright (c) 2011-2021 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook.chapter11

import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameGrabber}

import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.WindowConstants
import scala.concurrent.duration.Duration

/**
 * More sophisticated version of the `Ex1ReadVideoSequence` example.
 *
 * This version aims at playing video at correct playback speed. Delay is added to for slow frame rates.
 * For fast frame rates, some frames are skipped.
 *
 * A set of stopwatches is used to measure playback speed (playStopWatch). The intention is to match playback time and
 * video frame timestamps. If the playback would get ahead of the frame timestamp a delay is added.
 * If the playback falls behind the frame timestamp a time consuming operation, frame display or frame grab, is skipped.
 *
 * Additional stopwatches are used to continuously measure time taken by the most time consuming operations:
 * grabbing frames (grabStopWatch), displaying frames (displayStopWatch), and skipping frames (setTimestampStopWatch).
 * They are used to estimate how many frames we need to skip so the playback time can keep yo with the frame timestamps.
 */
object DisplayVideoSequence extends App {

  private val debugMode  = true
  private val frameScale = 0.5

  def debug(str: String): Unit = {
    if (debugMode) {
      println(str)
    }
  }

  // Use command line path, if provided
  private val inputFile = args
    .headOption
    .map(new File(_))
    .getOrElse(new File("data/bike.avi"))

  // Open video video file
  private val grabber = new FFmpegFrameGrabber(inputFile)
  grabber.start()

  // Prepare window to display frames
  private val canvasFrame = new CanvasFrame("Extracted Frame", 1)
  canvasFrame.setCanvasScale(frameScale)

  // Exit the example when the canvas frame is closed
  canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  private val frameDuration = Duration(math.round((1000 * 1000 * 1000) / grabber.getFrameRate), TimeUnit.NANOSECONDS)

  println(s"Frame rate  : ${grabber.getFrameRate}")
  println(s"Frame length: ${frameDuration.toMillis} milliseconds")

  private val playStopWatch                 = new StopWatch()
  private val grabStopWatch                 = new StopWatch()
  private val displayStopWatch              = new StopWatch()
  private val setTimestampStopWatch         = new StopWatch()
  private var frameCount                    = 0L
  private var skippedDisplayFrames          = 0L
  private var skippedGrabFrames             = 0L
  private var lastAbsTimeStampDeltaMicros   = 0d
  private var increasingTimeStampDeltaCount = 0

  // Read frame by frame, stop early if the display window is closed
  private var frame = grabStopWatch.add {
    grabber.grab()
  }
  playStopWatch.start()
  while (frame != null && canvasFrame.isVisible) {

    if (frame.image != null) {

      // Play time and frame time should be in line
      // If we are falling behind will need to skip display or skip ahead to catch up
      val timeStampDeltaMicros = frame.timestamp - playStopWatch.durationMicros
      if (timeStampDeltaMicros >= 0 || frameCount < 1) {

        // If we are ahead we will need to slow down to keep correct display frame rate
        val delayNanos   = math.max(0, (timeStampDeltaMicros * 1000).toLong)
        val delayMillis  = delayNanos / (1000L * 1000L)
        val delayExtraNS = (delayNanos % (1000L * 1000L)).toInt
        // Wait for the next frame
        Thread.sleep(delayMillis, delayExtraNS)

        // Now ready to display the frame
        displayStopWatch.add {
          canvasFrame.showImage(frame)
        }

        // Rest falling-behind indicator
        increasingTimeStampDeltaCount = 0
      } else {
        // If we are more then frame duration behind we may need to skip frames
        if (-timeStampDeltaMicros > frameDuration.toMicros) {

          // First test if we could recover only skipping frame display
          // We will mark that we are falling behind
          val absTimeStampDeltaMicros = math.abs(timeStampDeltaMicros)
          if (absTimeStampDeltaMicros >= lastAbsTimeStampDeltaMicros)
            increasingTimeStampDeltaCount += 1
          lastAbsTimeStampDeltaMicros = absTimeStampDeltaMicros

          // If are behind too many times in a row, we will try to recover by skipping frames
          if (increasingTimeStampDeltaCount > 1) {
            debug("> Skipping frames using setTimeframe")
            increasingTimeStampDeltaCount = 0

            // Display current frame
            displayStopWatch.start()
            canvasFrame.showImage(frame)
            displayStopWatch.stop()

            debug(f"  displayStopWatch     : ${displayStopWatch.averageDuration.toMillis}%3d ms")
            debug(f"  grabStopWatch        : ${grabStopWatch.averageDuration.toMillis}%3d ms")
            debug(f"  setTimestampStopWatch: ${setTimestampStopWatch.averageDuration.toMillis}%3d ms")

            // Calculate how many frames to skip based on timing of part operations
            val timeOverhead =
              absTimeStampDeltaMicros + (
                displayStopWatch.averageDuration
                  + grabStopWatch.averageDuration
                  + setTimestampStopWatch.averageDuration
              ).toMicros
            val framesToSkip      = math.ceil(timeOverhead / frameDuration.toMicros).toInt
            val timestampToSkipTo = frame.timestamp + framesToSkip * frameDuration.toMicros

            debug(s"  frame.timestamp  : ${frame.timestamp}")
            debug(s"  framesToSkip     : $framesToSkip")
            debug(s"  timestampToSkipTo: $timestampToSkipTo")
            debug(s"  skip delta       : ${(timestampToSkipTo - frame.timestamp) / 1000} ms")

            // Skip forward
            setTimestampStopWatch.add {
              grabber.setTimestamp(timestampToSkipTo)
            }

            skippedGrabFrames += framesToSkip
            frameCount += (framesToSkip - 1)
          } else {
            skippedDisplayFrames += 1
          }
        } else {
          skippedDisplayFrames += 1
        }
      }

      frameCount += 1
    }

    // Grab next frame and record execution time
    frame = grabStopWatch.add {
      grabber.grab()
    }
  }
  playStopWatch.stop()

  // Print some summary information about the playback
  println(f"Frame count           : $frameCount%5d")
  println(f"Skipped display frames: $skippedDisplayFrames%5d")
  println(f"Skipped grab frames   : $skippedGrabFrames%5d")
  if (frameCount > 0) {
    println(f"Average frame grab    : ${grabStopWatch.averageDuration.toMillis}%3d milliseconds")
    println(f"Average frame display : ${displayStopWatch.averageDuration.toMillis}%3d milliseconds")
    println(f"Average frame interval: ${playStopWatch.durationMillis / frameCount}%3.0f milliseconds")
    println(f"Average setTimestamp  : ${setTimestampStopWatch.averageDuration.toMillis}%3d milliseconds")
    println(f"Count of setTimestamp : ${setTimestampStopWatch.intervalCount}%3d")
  }

  // Close the video file
  grabber.release()
}
