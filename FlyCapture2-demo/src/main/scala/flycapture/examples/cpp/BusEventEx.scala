/*
 * Copyright (c) 2011-2014 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp

import java.util.Date

import org.bytedeco.javacpp.FlyCapture2._
import org.bytedeco.javacpp.{IntPointer, Pointer}

/**
 * The BusEventsEx demonstrates how to Register for Bus Events such as Camera Arrival/Removal and Bus Resets
 *
 * Example of using FlyCapture2 C++ API. Based on BusEventsEx.vb example from FlyCapture SDK.
 *
 * @author Jarek Sacha 
 */
object BusEventEx extends App {

  def now = new Date().toString

  def id(v: Int) = {
    val r = new IntPointer(1)
    r.put(v)
    r
  }

  def toID(pParameter: Pointer): Int = {
    pParameter.asByteBuffer().get()
  }


  println("BusEventEx  " + now)

  printBuildInfo()

  val onBusReset = new BusEventCallback {
    override def call(pParameter: Pointer, serialNumber: Int) = {
      println(s"$now *** BUS RESET *** SN: $serialNumber, p : ${toID(pParameter)}")
    }
  }
  val onResetCallbackHandle = new CallbackHandle()

  val onBusArrival = new BusEventCallback {
    override def call(pParameter: Pointer, serialNumber: Int) = {
      println(s"$now *** BUS ARRIVAL *** SN: $serialNumber, p : ${toID(pParameter)}")
    }
  }
  // In C/C++ this would look as follows
  //   FlyCapture2::CallbackHandle onArrivalCallbackHandle;
  val onArrivalCallbackHandle = new CallbackHandle()

  val onBusRemoval = new BusEventCallback {
    override def call(pParameter: Pointer, serialNumber: Int) = {
      println(s"$now *** BUS REMOVAL *** SN: $serialNumber, p : ${toID(pParameter)}")
    }
  }
  val onRemovalCallbackHandle = new CallbackHandle()

  // Register callbacks
  // The `id` is used here for debugging, to see if correct callbacks are registered.
  val busMgr = new BusManager()
  busMgr.RegisterCallback(onBusReset, BUS_RESET, id(1), onResetCallbackHandle)
  busMgr.RegisterCallback(onBusArrival, ARRIVAL, id(2), onArrivalCallbackHandle)
  busMgr.RegisterCallback(onBusRemoval, REMOVAL, id(3), onRemovalCallbackHandle)

  // Wait for the user to plug or unplug cameras to see callback notifications
  println("\nConnect or disconnect camera to see calback notifications.\n" +
    "Press the Enter key to exit.")
  readLine()

  // Un-register callbacks
  busMgr.UnregisterCallback(onResetCallbackHandle)
  busMgr.UnregisterCallback(onArrivalCallbackHandle)
  busMgr.UnregisterCallback(onRemovalCallbackHandle)

  println("Done.")
}
