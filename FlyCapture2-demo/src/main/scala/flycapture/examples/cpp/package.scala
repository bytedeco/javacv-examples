/*
 * Copyright (c) 2014 Jarek Sacha. All Rights Reserved.
 * Author's e-mail: jpsacha at gmail dot com
 */
package flycapture.examples

import org.bytedeco.javacpp.FlyCapture2
import org.bytedeco.javacpp.FlyCapture2._

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
    println("Checking -> " + errorMessage)
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

  def printPropertyInfo(cam: Camera): Unit = {
    val props = List(
      BRIGHTNESS -> "BRIGHTNESS",
      AUTO_EXPOSURE -> "AUTO_EXPOSURE",
      SHARPNESS -> "SHARPNESS",
      WHITE_BALANCE -> "WHITE_BALANCE",
      HUE -> "HUE",
      GAMMA -> "GAMMA",
      IRIS -> "IRIS",
      FOCUS -> "FOCUS",
      ZOOM -> "ZOOM",
      PAN -> "PAN",
      TILT -> "TILT",
      SHUTTER -> "SHUTTER",
      GAIN -> "GAIN",
      TRIGGER_MODE -> "TRIGGER_MODE",
      TRIGGER_DELAY -> "TRIGGER_DELAY",
      FRAME_RATE -> "FRAME_RATE",
      TEMPERATURE -> "TEMPERATURE"
    )

    props.foreach { case (t, n) => printPropertyInfo(cam, t, n)}
  }

  def printPropertyInfo(cam: Camera, propType: Int, propName: String): Unit = {
    val propertyInfo = new PropertyInfo(propType)
    check(cam.GetPropertyInfo(propertyInfo), "cam.GetPropertyInfo")
    println("Property info " + propName + " [present : " + propertyInfo.present + "]")
    if (propertyInfo.present) {
      println("  autoSupported    : " + propertyInfo.autoSupported)
      println("  manualSupported  : " + propertyInfo.manualSupported)
      println("  onOffSupported   : " + propertyInfo.onOffSupported)
      println("  onePushSupported : " + propertyInfo.onePushSupported)
      println("  absValSupported  : " + propertyInfo.absValSupported)
      println("  readOutSupported : " + propertyInfo.readOutSupported)
      if (propertyInfo.readOutSupported) {
        println("  min              : " + propertyInfo.min)
        println("  max              : " + propertyInfo.max)
        println("  absMin           : " + propertyInfo.absMin)
        println("  absMax           : " + propertyInfo.absMax)
        println("  pUnits           : " + propertyInfo.pUnits.getString)
        println("  pUnitAbbr        : " + propertyInfo.pUnitAbbr.getString)
        val property = new Property(propType)
        check(cam.GetProperty(property), "cam.GetProperty")
        println("  property value")
        println("    absControl     : " + property.absControl)
        println("    absValue       : " + property.absValue)
        println("    autoManualMode : " + property.autoManualMode)
        println("    onePush        : " + property.onePush)
        println("    onOff          : " + property.onOff)
        println("    present        : " + property.present)
        println("    type           : " + property.`type`)
        println("    valueA         : " + property.valueA)
        println("    valueB         : " + property.valueB)
      }
    }
  }

}
