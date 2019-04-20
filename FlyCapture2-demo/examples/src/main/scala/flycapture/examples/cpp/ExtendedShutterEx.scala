/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */

package flycapture.examples.cpp

import flycapture.CheckMacro.check
import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._
import org.bytedeco.javacpp.IntPointer

/**
 * The ExtendedShutterEx example program demonstrates how to enable and calculate extended integration times for
 * the camera. The way this is done can differ between cameras.
 *
 * Many applications require extended shutter (integration) times up to several seconds long.
 * Most Point Grey Imaging Products implement extended shutter functionality in one of two ways:
 *
 * 1. By turning off the FRAME_RATE register 0x83C. This effectively stops the camera from transmitting images at
 * fixed frame intervals; the frame rate becomes dependent on the shutter time.
 *
 * 2. By enabling extended shutter via the `EXTENDED_SHUTTER` register 0x1028.
 *
 * The program begins by initializing the first camera on the bus and uses `GetProperty()` to determine if it
 * implements the `FRAME_RATE` register. If it does, it turns the frame rate off.
 * If the camera does not implement this register, the program then checks to see if the camera implements
 * the `EXTENDED_SHUTTER` register. If it does, it accesses this register to put the camera into extended shutter mode.
 * Otherwise, the user is notified that the camera does not implement extended shutter and the program exits.
 *
 * Once the camera is in extended shutter mode, it is started in the default mode and frame rate.
 * A series of images are grabbed, and their timestamps printed as a way of verifying that
 * the extended shutter is working.
 *
 * Example of using FlyCapture2 C++ API. Based on ExtendedShutterEx.cpp example from FlyCapture SDK.
  *
  * @author Jarek Sacha
 */
object ExtendedShutterEx extends App {

  object ExtendedShutterType extends Enumeration {
    type ExtendedShutterType = Value
    val NO_EXTENDED_SHUTTER, DRAGONFLY_EXTENDED_SHUTTER, GENERAL_EXTENDED_SHUTTER = Value
  }


  println("Extended Shutter Example")

  val busMgr = new BusManager()
  val numCameras = {
    val numCamerasPtr = new IntPointer(1L)
    check(busMgr.GetNumOfCameras(numCamerasPtr))
    numCamerasPtr.get()
  }
  println("Number of cameras detected: " + numCameras)
  if (numCameras < 1) {
    println("Insufficient number of cameras... exiting.")
    System.exit(-1)
  }

  println("Selecting first detected camera.")
  val guid = new PGRGuid()
  check(busMgr.GetCameraFromIndex(0, guid))

  // Connect to a camera
  val cam = new Camera()
  check(cam.Connect(guid))

  try {
    val cameraInfo = new CameraInfo()
    check(cam.GetCameraInfo(cameraInfo))
    printCameraInfo(cameraInfo)

    // Check if the camera supports the FRAME_RATE property
    val propInfo = new PropertyInfo()
    propInfo.`type`(FRAME_RATE)
    check(cam.GetPropertyInfo(propInfo))

    // Detect shutter type
    val shutterType: ExtendedShutterType.Value =
      if (propInfo.present) {
        // Turn off frame rate
        val prop = new Property()
        prop.`type`(FRAME_RATE)
        check(cam.GetProperty(prop))

        prop.autoManualMode(false)
        prop.onOff(false)

        check(cam.SetProperty(prop))

        ExtendedShutterType.GENERAL_EXTENDED_SHUTTER
      } else {
        // Frame rate property does not appear to be supported.
        // Disable the extended shutter register instead.
        // This is only applicable for Dragonfly.
        //
        val k_extendedShutter = 0x1028
        val extendedShutterRegVal = new Array[Int](0)
        //
        check(cam.ReadRegister(k_extendedShutter, extendedShutterRegVal))

        // Check if 31st bit is set
        //        std::bitset<32> extendedShutterBS((int) extendedShutterRegVal );
        //        if ( extendedShutterBS[31] == true )
        if ((extendedShutterRegVal(0) & (1 << 31)) != 0) {
          // Set the camera into extended shutter mode
          check(cam.WriteRegister(k_extendedShutter, 0x80020000))
          ExtendedShutterType.DRAGONFLY_EXTENDED_SHUTTER
        }
        else {
          ExtendedShutterType.NO_EXTENDED_SHUTTER
        }
      }

    if (shutterType == ExtendedShutterType.NO_EXTENDED_SHUTTER) {
      println("Frame rate and extended shutter are not supported... exiting")
      System.exit(-1)
    }

    // Set the shutter property of the camera
    val prop = new Property()
    prop.`type`(SHUTTER)
    check(cam.GetProperty(prop))

    prop.autoManualMode(false)
    prop.absControl(true)

    val k_shutterVal = 3000.0f
    prop.absValue(k_shutterVal)

    check(cam.SetProperty(prop))

    println(f"Shutter time set to $k_shutterVal%7.2f ms")

    check(cam.GetProperty(prop))
    println(f"Actcual shutter     ${prop.absValue()}%7.2f ms")

    propInfo.`type`(SHUTTER)
    check(cam.GetPropertyInfo(propInfo))
    println(f"Shutter min         ${propInfo.absMin()}%7.2f ms")
    println(f"Shutter max         ${propInfo.absMax()}%7.2f ms")

    // Enable time-stamping
    val embeddedInfo = new EmbeddedImageInfo()
    check(cam.GetEmbeddedImageInfo(embeddedInfo))
    if (embeddedInfo.timestamp.available) embeddedInfo.timestamp.onOff(true)
    check(cam.SetEmbeddedImageInfo(embeddedInfo))

    // Start the camera
    check(cam.StartCapture())

    val k_numImages = 5
    for (_ <- 0 until k_numImages) {
      val image = new Image()
      check(cam.RetrieveBuffer(image))

      val timestamp = image.GetTimeStamp()
      println(f"TimeStamp [${timestamp.cycleSeconds} ${timestamp.cycleCount}]")
    }

    // Stop capturing images
    check(cam.StopCapture())

  } finally {
    println("Disconnecting camera.")
    cam.Disconnect()
  }

  println("Done!")
}
