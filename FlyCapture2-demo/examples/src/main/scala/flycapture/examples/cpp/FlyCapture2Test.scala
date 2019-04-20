/*
 * Copyright (c) 2011-2019 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */
package flycapture.examples.cpp

import flycapture.CheckMacro.check
import org.bytedeco.flycapture.FlyCapture2._
import org.bytedeco.flycapture.global.FlyCapture2._

/**
 * The FlyCapture2Test sample program is a simple program designed to report information related to all compatible
 * cameras attached to the host system, capture a series of images from a single camera,
 * record the amount of time taken to grab these images, then save the last image in the current directory.
 *
 * Example of using FlyCapture2 C++ API. Based on FlyCapture2Test.cpp example from FlyCapture SDK.
 */
object FlyCapture2Test extends App {

  def runSingleCamera(guid: PGRGuid) {
    val numImages = 10

    // Connect to a camera
    val cam = new Camera()
    check(cam.Connect(guid))

    // Get the camera information
    val camInfo = new CameraInfo()
    check(cam.GetCameraInfo(camInfo))
    printCameraInfo(camInfo)

    // Start capturing images
    check(cam.StartCapture())

    val rawImage = new Image()
    for (imageCnt <- 0 until numImages) {
      // Retrieve an image
      check(cam.RetrieveBuffer(rawImage))

      printf("Grabbed image %d\n", imageCnt)

      // Create a converted image
      val convertedImage = new Image()

      // Convert the raw image
      check(rawImage.Convert(PIXEL_FORMAT_MONO8, convertedImage))

      // Create a unique filename
      val filename = "FlyCapture2Test-%d-%d.pgm".format(camInfo.serialNumber, imageCnt)
      //
      // Save the image. If a file format is not passed in, then the file
      // extension is parsed to attempt to determine the file format.
      check(convertedImage.Save(filename))
    }

    // Stop capturing images
    check(cam.StopCapture())

    // Disconnect the camera
    check(cam.Disconnect())
  }


  printBuildInfo()

  val busMgr = new BusManager()
  val numCameras = Array[Int](0)
  check(busMgr.GetNumOfCameras(numCameras))
  println("Number of cameras detected: " + numCameras(0))

  for (i <- 0 until numCameras(0)) {
    val guid = new PGRGuid()
    check(busMgr.GetCameraFromIndex(i, guid))

    runSingleCamera(guid)
  }

  println("Done!")
}
