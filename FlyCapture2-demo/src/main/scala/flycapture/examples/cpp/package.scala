/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 * Author's e-mail: jpsacha at gmail dot com
 */
package flycapture.examples

import org.bytedeco.javacpp.FlyCapture2
import org.bytedeco.javacpp.FlyCapture2.{CameraInfo, Utilities, FC2Version, Error}

/**
 * Helper functions for C++ API examples.
 */
package object cpp {

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

  /**
   * Query and print version of FlyCapture SDK.
   */
  def printBuildInfo() {
    val version = new FC2Version()
    Utilities.GetLibraryVersion(version)

    println(s"FlyCapture2 library version: ${version.major}.${version.minor}.${version.`type`}.${version.build}")
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

}
