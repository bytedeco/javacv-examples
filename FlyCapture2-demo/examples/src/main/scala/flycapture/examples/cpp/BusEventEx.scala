/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp

import java.util.Date

import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._
import org.bytedeco.javacpp.{IntPointer, Pointer}

import scala.io.StdIn.readLine


/**
 * The BusEventsEx demonstrates how to register for Bus Events such as Camera Arrival/Removal and Bus Resets.
 *
 * Example of using FlyCapture2 C++ API. Based on BusEventsEx.vb example from FlyCapture SDK.
 *
 * @author Jarek Sacha 
 */
object BusEventEx extends App {

  def now: String = new Date().toString

  def id(v: Int): IntPointer = {
    val r = new IntPointer(1L)
    r.put(v)
    r
  }

  def toID(pParameter: Pointer): Int = pParameter.asByteBuffer().get()


  println("BusEventEx  " + now)

  printBuildInfo()

  // NOTE: We will be using a single callback method rather than a separate one for each event type.
  // This is due to current JavaCPP limitation
  // [[https://groups.google.com/d/msg/javacpp-project/bxTAlvLKn0M/SUS0z4qMvyAJ]]
  val busEventCallback = new BusEventCallback {
    override def call(pParameter: Pointer, serialNumber: Int): Unit = {
      val eventName = toID(pParameter) match {
        case BUS_RESET => "BUS_RESET"
        case ARRIVAL => "ARRIVAL"
        case REMOVAL => "REMOVAL"
        case _ => "?"
      }
      println(f"$now *** $eventName%-9s *** SN: $serialNumber")
    }
  }

  // We need separate handle for each registered callback to unregister it.
  val onResetCallbackHandle = new CallbackHandle()
  val onArrivalCallbackHandle = new CallbackHandle()
  val onRemovalCallbackHandle = new CallbackHandle()

  // Register callbacks
  // The `id` is used for distinguishing event types.
  val busMgr = new BusManager()
  busMgr.RegisterCallback(busEventCallback, BUS_RESET, id(BUS_RESET), onResetCallbackHandle)
  busMgr.RegisterCallback(busEventCallback, ARRIVAL, id(ARRIVAL), onArrivalCallbackHandle)
  busMgr.RegisterCallback(busEventCallback, REMOVAL, id(REMOVAL), onRemovalCallbackHandle)

  // Wait for the user to plug or unplug cameras to see callback notifications
  println("\nConnect or disconnect camera to see callback notifications.\n" +
    "Press the Enter key to exit.")
  readLine()

  // Un-register callbacks
  busMgr.UnregisterCallback(onResetCallbackHandle)
  busMgr.UnregisterCallback(onArrivalCallbackHandle)
  busMgr.UnregisterCallback(onRemovalCallbackHandle)

  println("Done.")
}
