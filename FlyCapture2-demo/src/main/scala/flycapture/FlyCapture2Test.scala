/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 * Author's e-mail: jpsacha at gmail dot com
 */
package flycapture

import org.bytedeco.javacpp.FlyCapture2
import org.bytedeco.javacpp.FlyCapture2._

/**
 * Example of using FlyCapture2 C++ API.
 * Based on FlyCapture2Test.cpp example from FlyCapture SDK.
 */
object FlyCapture2Test extends App {

  class FC2Exception(message: String, val error: Error) extends Exception(message: String)

  /**
   * Invokes the method and checks the return code.
   * If the return code is different than `FC2_ERROR_OK` it throws an exception.
   * Helper for using FlyCapture2 C API.
   *
   * @param method Method to be checked.
   * @param errorMessage Error message used in the exception (if exception is thrown).
   */
  def check(method: => Error, errorMessage: String) {
    val err = method
    if (err.GetType() != FlyCapture2.PGRERROR_OK) {
      val errorDesc = err.GetDescription().getString
      throw new FC2Exception("Err: " + err.GetType() + " " + errorMessage + "\nInternal error description: " + errorDesc, err)
    }
  }

  def printBuildInfo(): Unit = {
    val fc2Version = new FC2Version()
    Utilities.GetLibraryVersion(fc2Version)

    println(s"FlyCapture2 library version: ${fc2Version.major}.${fc2Version.minor}.${fc2Version.`type`}.${fc2Version.build}")
  }

  def printCameraInfo(camInfo: CameraInfo) {
    println("\n*** CAMERA INFORMATION ***\n" +
      s"Serial number       - ${camInfo.serialNumber} \n" +
      s"Camera model        - ${camInfo.modelName.getString}\n" +
      s"Camera vendor       - ${camInfo.vendorName.getString}\n" +
      s"Sensor              - ${camInfo.sensorInfo.getString}\n" +
      s"Resolution          - ${camInfo.sensorResolution.getString}\n" +
      s"Firmware version    - ${camInfo.firmwareVersion.getString}\n" +
      s"Firmware build time - ${camInfo.firmwareBuildTime.getString}\n")
  }

  def runSingleCamera(guid: PGRGuid) {
    val k_numImages = 10


    // Connect to a camera
    val cam = new Camera()
    check(cam.Connect(guid), " - Connect")

    // Get the camera information
    val camInfo = new CameraInfo()
    check(cam.GetCameraInfo(camInfo), " - GetCameraInfo")
    printCameraInfo(camInfo)

    // Start capturing images
    check(cam.StartCapture(), " - StartCapture")

    val rawImage = new Image()
    for (imageCnt <- 0 until k_numImages) {
      // Retrieve an image
      check(cam.RetrieveBuffer(rawImage), " - RetrieveBuffer")

      printf("Grabbed image %d\n", imageCnt)

      // Create a converted image
      val convertedImage = new Image()

      // Convert the raw image
      check(rawImage.Convert(PIXEL_FORMAT_MONO8, convertedImage), " - Convert")

      // Create a unique filename
      val filename = "FlyCapture2Test-%d-%d.pgm".format(camInfo.serialNumber, imageCnt)
      //
      // Save the image. If a file format is not passed in, then the file
      // extension is parsed to attempt to determine the file format.
      check(convertedImage.Save(filename), " - Save")
    }

    // Stop capturing images
    check(cam.StopCapture(), " - StopCapture")

    // Disconnect the camera
    check(cam.Disconnect(), " - Disconnect")
  }


  printBuildInfo()

  val busMgr = new BusManager()
  val numCameras = Array[Int](0)
  check(busMgr.GetNumOfCameras(numCameras), " - GetNumOfCameras")
  println("Number of cameras detected: " + numCameras(0))

  for (i <- 0 until numCameras(0)) {
    val guid = new PGRGuid()
    check(busMgr.GetCameraFromIndex(i, guid), " - GetCameraFromIndex")

    runSingleCamera(guid)
  }

  println("Done!")
}
