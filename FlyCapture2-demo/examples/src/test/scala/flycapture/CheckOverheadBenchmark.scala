/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture

import flycapture.CheckMacro._
import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._
import org.bytedeco.javacpp.IntPointer

import scala.math.min

/**
 * Theoretically using `CheckMacro.ckeck` to test for error condition may have
 * some overhead compared to just using more cumbersome `if`.
 * Practically there is no difference that can be detected.
 *
 * What makes some minimal difference is using "IntPointer" instead of "Array[Int]" as argument to
 * FlyCapture2 method.
 * The difference is really small but consistent, "IntPointer" is about 0.0002 ms faster on tested system.
 * That is only two ten thousandth of a milli second.
 *
 * @author Jarek Sacha 
 */
object CheckOverheadBenchmark extends App {

  val nbIterations = 100000

  // Warm-up
  println("Warm-up")
  testIf()
  testCheck()

  println("Run")
  var minCheck = Double.PositiveInfinity
  var minIf = Double.PositiveInfinity
  for (_ <- 0 until 10) {
    minCheck = min(testCheck(), minCheck)
    minIf = min(testIf(), minIf)
  }
  println()
  println("Min average IF   : " + minIf + " ns")
  println("Min average CHECK: " + minCheck + " ns")


  def testCheck(): Double = {
    val busMgr = new BusManager()
    //        val numCameras = new Array[Int](1)
    val numCameras = new IntPointer(1L)

    val t0 = System.nanoTime()
    for (_ <- 0 until nbIterations) {
      check(busMgr.GetNumOfCameras(numCameras))
    }
    val t1 = System.nanoTime()
    val tAverage = (t1 - t0).toDouble / nbIterations
    println("CHECK average time: " + tAverage + " ns")
    tAverage
  }

  def testIf(): Double = {
    val busMgr = new BusManager()
    //    val numCameras = new Array[Int](1)
    val numCameras = new IntPointer(1L)

    val t0 = System.nanoTime()
    for (_ <- 0 until nbIterations) {
      if (busMgr.GetNumOfCameras(numCameras).GetType() != PGRERROR_OK) {
        throw new Exception("Error in ...")
      }
    }
    val t1 = System.nanoTime()
    val tAverage = (t1 - t0).toDouble / nbIterations
    println("IF average time   : " + tAverage + " ns")
    tAverage
  }

}
