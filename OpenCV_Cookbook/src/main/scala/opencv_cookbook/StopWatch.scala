/*
 * Copyright (c) 2021-2022 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package opencv_cookbook

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
  * StopWatch that can track of repeated start/stop intervals.
  */
class StopWatch {
  private var accumulatedDuration = Duration.Zero
  private var startTime: Option[Long] = None
  private var stopCount: Long = 0L

  def start(): Unit = {
    startTime = Option(System.nanoTime())
  }

  def stop(): Unit = {
    startTime match {
      case Some(t0) =>
        accumulatedDuration = accumulatedDuration + Duration(System.nanoTime() - t0, TimeUnit.NANOSECONDS)
        startTime = None
        stopCount += 1
      case None =>
        throw new IllegalStateException("StopWatch: cannot stop, not started.")
    }
  }

  def reset(): Unit = {
    accumulatedDuration = Duration.Zero
    startTime = None
    stopCount = 0
  }

  def add[R](op: => R): R = {
    start()
    val r = op
    stop()
    r
  }

  def duration: Duration = {
    startTime match {
      case Some(t0) =>
        accumulatedDuration + Duration(System.nanoTime() - t0, TimeUnit.NANOSECONDS)
      case None =>
        accumulatedDuration
    }
  }

  def durationMillis: Double = duration.toUnit(TimeUnit.MILLISECONDS)

  def durationMicros: Double = duration.toUnit(TimeUnit.MICROSECONDS)

  def averageDuration: Duration = {
    if (startTime.isEmpty) {
      if (stopCount == 0) Duration.Zero else duration / stopCount.toDouble
    } else {
      throw new IllegalStateException("Cannot average when in stated state.")
    }
  }

  def intervalCount: Long = stopCount
}
