/*
 * Copyright (c) 2011-2021 Jarek Sacha. All Rights Reserved.
 *
 * Author's e-mail: jpsacha at gmail.com
 */
package flycapture.examples.c

import org.bytedeco.flycapture.FlyCapture2_C._
import org.bytedeco.flycapture.global.FlyCapture2_C._

/**
 * Example of using FlyCapture2 C API.
 * Based on FlyCapture2Test_C.c example from FlyCapture SDK.
 */
object FlyCapture2Test_C extends App {

  class FC2Exception(message: String, val errorCode: Int) extends Exception(message: String)

  /**
   * Invokes the method and checks the return code.
   * If the return code is different than `FC2_ERROR_OK` it throws an exception.
   * Helper for using FlyCapture2 C API.
   *
   * @param method Method to be checked.
   * @param errorMessage Error message used in the exception (if exception is thrown).
   */
  def check(method: => Int, errorMessage: String): Unit = {
    val err = method
    if (err != FC2_ERROR_OK) {
      val desc = fc2ErrorToDescription(err)
      throw new FC2Exception(
        "Err: " + err + " " + errorMessage + "\nInternal error description: " + desc.getString,
        err
      )
    }
  }

  def printCameraInfo(context: fc2Context): Unit = {
    val camInfo = new fc2CameraInfo()
    check(fc2GetCameraInfo(context, camInfo), " - in fc2GetCameraInfo")

    println("\n*** CAMERA INFORMATION ***\n" +
      s"Serial number       - ${camInfo.serialNumber} \n" +
      s"Camera model        - ${camInfo.modelName.getString}\n" +
      s"Camera vendor       - ${camInfo.vendorName.getString}\n" +
      s"Sensor              - ${camInfo.sensorInfo.getString}\n" +
      s"Resolution          - ${camInfo.sensorResolution.getString}\n" +
      s"Firmware version    - ${camInfo.firmwareVersion.getString}\n" +
      s"Firmware build time - ${camInfo.firmwareBuildTime.getString}\n")
  }

  def setTimeStamping(context: fc2Context, enableTimeStamp: Boolean): Unit = {
    val embeddedInfo = new fc2EmbeddedImageInfo()

    check(fc2GetEmbeddedImageInfo(context, embeddedInfo), " - in fc2GetEmbeddedImageInfo")
    if (embeddedInfo.timestamp.available != 0) {
      embeddedInfo.timestamp.onOff(if (enableTimeStamp) 1 else 0)
    }

    check(fc2SetEmbeddedImageInfo(context, embeddedInfo), " - in fc2SetEmbeddedImageInfo ")
  }

  def grabImages(context: fc2Context, numImagesToGrab: Int): Unit = {
    val rawImage = new fc2Image()

    check(fc2CreateImage(rawImage), " - in fc2CreateImage")

    val convertedImage = new fc2Image()
    check(fc2CreateImage(convertedImage), "in fc2CreateImage")

    // If externally allocated memory is to be used for the converted image,
    // simply assigning the pData member of the fc2Image structure is
    // insufficient. fc2SetImageData() should be called in order to populate
    // the fc2Image structure correctly. This can be done at this point,
    // assuming that the memory has already been allocated.

    var prevTimestamp = new fc2TimeStamp()
    for (_ <- 0 until numImagesToGrab) {
      // Retrieve the image
      check(fc2RetrieveBuffer(context, rawImage), " - in retrieveBuffer")
      // Get and print out the time stamp
      val ts: fc2TimeStamp = fc2GetImageTimeStamp(rawImage)
      val diff = (ts.cycleSeconds - prevTimestamp.cycleSeconds) * 8000 +
        (ts.cycleCount - prevTimestamp.cycleCount)
      prevTimestamp = ts
      println(s"timestamp [${ts.cycleSeconds} ${ts.cycleCount}] - $diff")
    }

    // Convert the final image to RGB
    check(fc2ConvertImageTo(FC2_PIXEL_FORMAT_BGR, rawImage, convertedImage), "- in fc2ConvertImageTo")

    // Save it to PNG
    printf("Saving the last image to fc2TestImage.png \n")
    check(
      fc2SaveImage(convertedImage, "fc2TestImage.png", FC2_PNG),
      "- in fc2SaveImage\nPlease check write permissions."
    )

    check(fc2DestroyImage(rawImage), " - in fc2DestroyImage")
    check(fc2DestroyImage(convertedImage), " - in fc2DestroyImage")
  }

  val version = new fc2Version()
  fc2GetLibraryVersion(version)

  println("version: %d.%d.%d.%d".format(version.major, version.minor, version.`type`, version.build))

  val context = new fc2Context(null)
  context.setNull()
  check(fc2CreateContext(context), " - in fc2CreateContext")

  val numCameras = Array[Int](0)
  check(fc2GetNumOfCameras(context, numCameras), "- in fc2GetNumOfCameras")
  println("numCameras: " + numCameras(0))

  val guid = new fc2PGRGuid()
  check(fc2GetCameraFromIndex(context, 0, guid), " - in fc2GetCameraFromIndex")

  check(fc2Connect(context, guid), " - in fc2Connect")

  printCameraInfo(context)

  setTimeStamping(context, enableTimeStamp = true)

  check(fc2StartCapture(context), "- in fc2StartCapture")

  grabImages(context, 100)

  check(fc2StopCapture(context), "- in fc2StopCapture")

  check(fc2DestroyContext(context), "- in fc2DestroyContext")
}
