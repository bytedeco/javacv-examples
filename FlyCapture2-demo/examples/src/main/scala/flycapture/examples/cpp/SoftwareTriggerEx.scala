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

import scala.io.StdIn.readLine

/**
 * Demonstrates some of the basic asynchronous trigger capabilities of compatible Point Grey Imaging Products.
 *
 * Example of using FlyCapture2 C++ API. Based on AsyncTriggerEx.cpp example from FlyCapture SDK.
 */
object SoftwareTriggerEx extends App {

  val triggerInquiryRegister = 0x530
  val softwareTriggerRegister = 0x62C
  val cameraPowerRegister = 0x610

  def checkSoftwareTriggerPresence(cam: Camera): Boolean = {
    val regValPtr = new IntPointer(1L)

    check(cam.ReadRegister(triggerInquiryRegister, regValPtr))

    (regValPtr.get & 0x10000) == 0x10000
  }

  def pollForTriggerReady(cam: Camera): Boolean = {
    var regVal = 0
    var ok = true
    do {
      val regVarPtr = new IntPointer(1L)
      val error = cam.ReadRegister(softwareTriggerRegister, regVarPtr)
      if (error.GetType() != PGRERROR_OK) {
        println(error.GetDescription().getString)
        error.PrintErrorTrace()
        ok = false
      } else {
        regVal = regVarPtr.get
      }

    } while ((regVal >> 31) != 0 && ok)

    ok
  }

  def fireSoftwareTrigger(cam: Camera) {
    val fireTriggerVal = 0x80000000
    check(cam.WriteRegister(softwareTriggerRegister, fireTriggerVal))
  }


  printBuildInfo()

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
    // Power on the camera
    println("Power on the camera")
    val cameraPowerVal = 0x80000000
    check(cam.WriteRegister(cameraPowerRegister, cameraPowerVal))

    // Wait for camera to complete power-up
    val regVal = new IntPointer(1L)
    var retries = 10
    do {
      Thread.sleep(100)
      val error = cam.ReadRegister(cameraPowerRegister, regVal)
      if (error.GetType() == PGRERROR_TIMEOUT) {
        // ignore timeout errors, camera may not be responding to
        // register reads during power-up
      } else {
        check(error)
      }
      retries -= 1
    } while ((regVal.get() & cameraPowerVal) == 0 && retries > 0)


    // Get the camera information
    val camInfo = new CameraInfo()
    check(cam.GetCameraInfo(camInfo))
    printCameraInfo(camInfo)

    // Get current trigger settings
    val triggerMode = new TriggerMode()
    check(cam.GetTriggerMode(triggerMode))

    // Set camera to trigger mode 0
    triggerMode.onOff(true)
    triggerMode.mode(0)
    triggerMode.parameter(0)
    // A source of 7 means software trigger
    triggerMode.source(7)

    check(cam.SetTriggerMode(triggerMode))

    // Poll to ensure camera is ready
    if (!pollForTriggerReady(cam)) {
      println("Error polling for trigger ready!")
      System.exit(-1)
    }

    // Get the camera configuration
    val config = new FC2Config()
    check(cam.GetConfiguration(config))

    // Set the grab timeout to 5 seconds
    config.grabTimeout(5000)

    // Set the camera configuration
    check(cam.SetConfiguration(config))

    // Camera is ready, start capturing images
    check(cam.StartCapture())

    if (!checkSoftwareTriggerPresence(cam)) {
      println("SOFT_ASYNC_TRIGGER not implemented on this camera!  Stopping application.")
      System.exit(-1)
    }

    val numImages = 3
    println(s"Capturing $numImages images...")
    for (imageCount <- 0 until numImages) {
      // Check that the trigger is ready
      pollForTriggerReady(cam)

      println("Press the Enter key to initiate a software trigger.")
      readLine()

      // Fire software trigger
      fireSoftwareTrigger(cam)

      // Retrieve an image
      val rawImage = new Image()
      check(cam.RetrieveBuffer(rawImage))

      printf("Grabbed image %d\n", imageCount)

      // Create a converted image
      val convertedImage = new Image()

      // Convert the raw image
      check(rawImage.Convert(PIXEL_FORMAT_MONO8, convertedImage))

      // Create a unique filename
      val filename = "SoftwareTriggerEx-%d-%d.pgm".format(camInfo.serialNumber, imageCount)
      //
      // Save the image. If a file format is not passed in, then the file
      // extension is parsed to attempt to determine the file format.
      check(convertedImage.Save(filename))
    }

    // Turn trigger mode off.
    triggerMode.onOff(false)
    check(cam.SetTriggerMode(triggerMode))
    println("Finished grabbing images")

    // Stop capturing images
    check(cam.StopCapture())

    // Turn trigger mode off.
    triggerMode.onOff(false)
    check(cam.SetTriggerMode(triggerMode))
    println("Finished grabbing images")
  } finally {
    println("Disconnecting camera.")
    cam.Disconnect()
  }

  println("Done!")
}
